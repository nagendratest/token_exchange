package de.telefonica.apigw.tokengenerator.service;

import de.telefonica.apigw.tokengenerator.config.AuthCacheProperties;
import de.telefonica.apigw.tokengenerator.config.EOrchestratorProperties;
import de.telefonica.apigw.tokengenerator.cache.InMemoryCache;
import de.telefonica.apigw.tokengenerator.dto.EOrchestratorTokenRequest;
import de.telefonica.apigw.tokengenerator.dto.EOrchestratorTokenResponse;
import de.telefonica.apigw.tokengenerator.exception.TokenAcquisitionException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class EOrchestratorService {

  private static final Logger log = LoggerFactory.getLogger(EOrchestratorService.class);

  private final RestClient restClient;
  private final EOrchestratorProperties properties;
  private final InMemoryCache<String, ResponseEntity<EOrchestratorTokenResponse>> cache;
  private final AuthCacheProperties cacheProperties;

  public EOrchestratorService(
      RestClient.Builder builder,
      EOrchestratorProperties properties,
      @Qualifier("eorchestratorCache")
      InMemoryCache<String, ResponseEntity<EOrchestratorTokenResponse>> eorchestratorCache,
      AuthCacheProperties cacheProperties
  ) {
    this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
    this.properties = properties;
    this.cache = eorchestratorCache;
    this.cacheProperties = cacheProperties;
  }

  public ResponseEntity<EOrchestratorTokenResponse> fetchToken(EOrchestratorTokenRequest request) {
    String cacheKey = cacheKey(request);
    ResponseEntity<EOrchestratorTokenResponse> cached = cache.getIfPresent(cacheKey);
    if (cached != null) {
      log.info("eorchestrator cacheHit=true tenantId={} user={}", request.tenantId(), request.username());
      return cached;
    }
    log.info(
        "eorchestrator cacheHit=false ttlSeconds={} tenantId={} user={} action=fetchUpstream",
        cacheProperties.getEorchestratorTtlSeconds(),
        request.tenantId(),
        request.username()
    );

    String rawAuth = request.username() + ":" + request.password();
    String encoded = Base64.getEncoder()
        .encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8));

    try {
      ResponseEntity<String> response = restClient.post()
          .uri(properties.getPath())
          .header(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
          .header("TenantId", request.tenantId())
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            int status = res.getStatusCode().value();
            if (status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
              throw new TokenAcquisitionException(
                  "EOrchestrator service unavailable: " + status, status);
            }
            throw new TokenAcquisitionException(
                "EOrchestrator error: " + status);
          })
          .toEntity(String.class);
      ResponseEntity<EOrchestratorTokenResponse> tokenResponse = toTokenResponse(response);
      if (response.getStatusCode().is2xxSuccessful()) {
        cache.put(cacheKey, tokenResponse);
      }
      return tokenResponse;
    } catch (RestClientResponseException ex) {
      int status = ex.getStatusCode().value();
      if (status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
        throw new TokenAcquisitionException(
            "EOrchestrator service unavailable: " + status, status, ex);
      }
      throw new TokenAcquisitionException(
          "EOrchestrator error: " + status, ex);
    } catch (ResourceAccessException ex) {
      throw new TokenAcquisitionException("EOrchestrator connection failed", ex);
    }
  }

  private String cacheKey(EOrchestratorTokenRequest request) {
    String material = "eorchestrator|" + properties.getBaseUrl() + "|" + properties.getPath()
        + "|" + request.tenantId() + "|" + request.username() + "|" + request.password();
    return sha256(material);
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException ex) {
      // SHA-256 is always available in the JRE; fallback to a UUID-based key if it ever fails.
      return UUID.randomUUID().toString();
    }
  }

  private ResponseEntity<EOrchestratorTokenResponse> toTokenResponse(ResponseEntity<String> response) {
    return ResponseEntity.status(response.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new EOrchestratorTokenResponse(response.getBody()));
  }
}
