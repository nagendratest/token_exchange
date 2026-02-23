package com.example.tokencache.service;

import com.example.tokencache.config.AuthCacheProperties;
import com.example.tokencache.config.EnmProperties;
import com.example.tokencache.cache.InMemoryCache;
import com.example.tokencache.dto.EnmLoginRequest;
import com.example.tokencache.exception.TokenAcquisitionException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class EnmService {

  private static final Logger log = LoggerFactory.getLogger(EnmService.class);

  private final RestClient restClient;
  private final EnmProperties properties;
  private final InMemoryCache<String, ResponseEntity<String>> cache;
  private final AuthCacheProperties cacheProperties;

  public EnmService(
      RestClient.Builder builder,
      EnmProperties properties,
      @Qualifier("enmCache") InMemoryCache<String, ResponseEntity<String>> enmCache,
      AuthCacheProperties cacheProperties
  ) {
    this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
    this.properties = properties;
    this.cache = enmCache;
    this.cacheProperties = cacheProperties;
  }

  public ResponseEntity<String> login(EnmLoginRequest request) {
    String cacheKey = cacheKey(request);
    ResponseEntity<String> cached = cache.getIfPresent(cacheKey);
    if (cached != null) {
      log.info("enm cacheHit=true user={}", request.username());
      return cached;
    }
    log.info(
        "enm cacheHit=false ttlSeconds={} user={} action=fetchUpstream",
        cacheProperties.getEnmTtlSeconds(),
        request.username()
    );

    String cookie = "IDToken1=" + request.username() + "; IDToken2=" + request.password();

    try {
      ResponseEntity<String> response = restClient.post()
          .uri(properties.getPath())
          .header("Cookie", cookie)
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            throw new TokenAcquisitionException("ENM error: " + res.getStatusCode().value());
          })
          .toEntity(String.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        ResponseEntity<String> cachedResponse = copyResponse(response);
        cache.put(cacheKey, cachedResponse);
        return cachedResponse;
      }
      return response;
    } catch (RestClientResponseException ex) {
      throw new TokenAcquisitionException("ENM error: " + ex.getStatusCode().value(), ex);
    } catch (ResourceAccessException ex) {
      throw new TokenAcquisitionException("ENM connection failed", ex);
    }
  }

  private String cacheKey(EnmLoginRequest request) {
    String material = "enm|" + properties.getBaseUrl() + "|" + properties.getPath()
        + "|" + request.username() + "|" + request.password();
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
      return UUID.randomUUID().toString();
    }
  }

  private ResponseEntity<String> copyResponse(ResponseEntity<String> response) {
    HttpHeaders headers = new HttpHeaders();
    headers.putAll(Objects.requireNonNull(response.getHeaders()));
    return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
  }
}
