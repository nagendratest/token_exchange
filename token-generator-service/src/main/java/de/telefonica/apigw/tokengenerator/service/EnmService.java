package de.telefonica.apigw.tokengenerator.service;

import de.telefonica.apigw.tokengenerator.cache.InMemoryCache;
import de.telefonica.apigw.tokengenerator.config.AuthCacheProperties;
import de.telefonica.apigw.tokengenerator.config.EnmProperties;
import de.telefonica.apigw.tokengenerator.dto.EnmLoginRequest;
import de.telefonica.apigw.tokengenerator.dto.EnmLoginResponse;
import de.telefonica.apigw.tokengenerator.exception.TokenAcquisitionException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class EnmService {

  private static final Logger log = LoggerFactory.getLogger(EnmService.class);
  private static final String ENM_TOKEN_COOKIE = "iPlanetDirectoryPro";

  private final RestClient restClient;
  private final EnmProperties properties;
  private final InMemoryCache<String, ResponseEntity<EnmLoginResponse>> cache;
  private final AuthCacheProperties cacheProperties;

  public EnmService(
      RestClient.Builder builder,
      EnmProperties properties,
      @Qualifier("enmCache") InMemoryCache<String, ResponseEntity<EnmLoginResponse>> enmCache,
      AuthCacheProperties cacheProperties
  ) {
    this.restClient = configureBuilder(builder, properties)
        .baseUrl(properties.getBaseUrl())
        .build();
    this.properties = properties;
    this.cache = enmCache;
    this.cacheProperties = cacheProperties;
  }

  public ResponseEntity<EnmLoginResponse> login(EnmLoginRequest request) {
    String cacheKey = cacheKey(request);
    ResponseEntity<EnmLoginResponse> cached = cache.getIfPresent(cacheKey);
    if (cached != null) {
      log.info(
          "enm cacheHit=true tenantId={} user={}",
          request.tenantId(),
          request.username()
      );
      return cached;
    }
    log.info(
        "enm cacheHit=false ttlSeconds={} tenantId={} user={} action=fetchUpstream",
        cacheProperties.getEnmTtlSeconds(),
        request.tenantId(),
        request.username()
    );

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("IDToken1", request.username());
    form.add("IDToken2", request.password());

    try {
      ResponseEntity<String> response = restClient.post()
          .uri(properties.getPath())
          .header("TenantId", request.tenantId())
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            int status = res.getStatusCode().value();
            if (status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
              throw new TokenAcquisitionException(
                  "ENM service unavailable: " + status, status);
            }
            throw new TokenAcquisitionException("ENM error: " + status);
          })
          .toEntity(String.class);
      String token = extractCookieValue(response.getHeaders(), ENM_TOKEN_COOKIE);
      if (token == null || token.isBlank()) {
        throw new TokenAcquisitionException("ENM token cookie missing", 502);
      }
      ResponseEntity<EnmLoginResponse> tokenResponse = ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(new EnmLoginResponse(token));
      cache.put(cacheKey, tokenResponse);
      return tokenResponse;
    } catch (RestClientResponseException ex) {
      int status = ex.getStatusCode().value();
      if (status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
        throw new TokenAcquisitionException(
            "ENM service unavailable: " + status, status, ex);
      }
      throw new TokenAcquisitionException("ENM error: " + status, ex);
    } catch (ResourceAccessException ex) {
      throw new TokenAcquisitionException("ENM connection failed", ex);
    }
  }

  static RestClient.Builder configureBuilder(RestClient.Builder builder, EnmProperties properties) {
    validateResolveConfiguration(properties);
    if (!requiresCustomTransport(properties)) {
      return builder;
    }
    return builder.requestFactory(buildRequestFactory(properties));
  }

  static DnsResolver buildDnsResolver(EnmProperties properties) throws UnknownHostException {
    InetAddress resolvedAddress = InetAddress.getByName(properties.getResolveAddress());
    return new DnsResolver() {
      @Override
      public InetAddress[] resolve(String host) throws UnknownHostException {
        if (host.equalsIgnoreCase(properties.getResolveHost())) {
          return new InetAddress[] {resolvedAddress};
        }
        return InetAddress.getAllByName(host);
      }

      @Override
      public String resolveCanonicalHostname(String host) throws UnknownHostException {
        if (host.equalsIgnoreCase(properties.getResolveHost())) {
          return properties.getResolveHost();
        }
        return InetAddress.getByName(host).getCanonicalHostName();
      }
    };
  }

  static int effectivePort(String baseUrl) {
    URI uri = URI.create(baseUrl);
    if (uri.getPort() != -1) {
      return uri.getPort();
    }
    if ("https".equalsIgnoreCase(uri.getScheme())) {
      return 443;
    }
    if ("http".equalsIgnoreCase(uri.getScheme())) {
      return 80;
    }
    return -1;
  }

  private static boolean requiresCustomTransport(EnmProperties properties) {
    return hasResolveOverride(properties) || properties.isInsecureTls();
  }

  private static boolean hasResolveOverride(EnmProperties properties) {
    return StringUtils.hasText(properties.getResolveHost())
        && StringUtils.hasText(properties.getResolveAddress());
  }

  private static void validateResolveConfiguration(EnmProperties properties) {
    boolean hasResolveHost = StringUtils.hasText(properties.getResolveHost());
    boolean hasResolveAddress = StringUtils.hasText(properties.getResolveAddress());
    if (hasResolveHost != hasResolveAddress) {
      throw new IllegalStateException(
          "ENM resolveHost and resolveAddress must be configured together"
      );
    }
    if (!hasResolveOverride(properties)) {
      return;
    }

    URI baseUri = URI.create(properties.getBaseUrl());
    String baseHost = baseUri.getHost();
    if (!properties.getResolveHost().equalsIgnoreCase(baseHost)) {
      throw new IllegalStateException(
          "ENM resolveHost must match the host in enm.base-url"
      );
    }

    if (properties.getResolvePort() != null) {
      int basePort = effectivePort(properties.getBaseUrl());
      if (basePort != properties.getResolvePort()) {
        throw new IllegalStateException(
            "ENM resolvePort must match the port implied by enm.base-url"
        );
      }
    }
  }

  private static HttpComponentsClientHttpRequestFactory buildRequestFactory(EnmProperties properties) {
    try {
      PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder =
          PoolingHttpClientConnectionManagerBuilder.create();
      if (hasResolveOverride(properties)) {
        connectionManagerBuilder.setDnsResolver(buildDnsResolver(properties));
      }
      if (properties.isInsecureTls()) {
        connectionManagerBuilder.setSSLSocketFactory(buildInsecureSslSocketFactory());
      }
      CloseableHttpClient httpClient = HttpClients.custom()
          .setConnectionManager(connectionManagerBuilder.build())
          .build();
      return new HttpComponentsClientHttpRequestFactory(httpClient);
    } catch (GeneralSecurityException | UnknownHostException ex) {
      throw new IllegalStateException("Failed to configure ENM HTTP client", ex);
    }
  }

  private static SSLConnectionSocketFactory buildInsecureSslSocketFactory()
      throws GeneralSecurityException {
    return new SSLConnectionSocketFactory(
        SSLContexts.custom().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build(),
        NoopHostnameVerifier.INSTANCE
    );
  }

  private String cacheKey(EnmLoginRequest request) {
    String material = "enm|" + properties.getBaseUrl() + "|" + properties.getPath()
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
      return UUID.randomUUID().toString();
    }
  }

  private String extractCookieValue(HttpHeaders headers, String cookieName) {
    if (headers == null || cookieName == null || cookieName.isBlank()) {
      return null;
    }
    List<String> setCookies = headers.get(HttpHeaders.SET_COOKIE);
    if (setCookies == null) {
      return null;
    }
    for (String header : setCookies) {
      String value = parseCookieValue(header, cookieName);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String parseCookieValue(String header, String cookieName) {
    if (header == null || header.isBlank()) {
      return null;
    }
    String needle = cookieName + "=";
    int start = header.indexOf(needle);
    if (start < 0) {
      return null;
    }
    int valueStart = start + needle.length();
    int valueEnd = header.indexOf(';', valueStart);
    if (valueEnd < 0) {
      valueEnd = header.length();
    }
    if (valueStart >= valueEnd) {
      return null;
    }
    return header.substring(valueStart, valueEnd);
  }
}

