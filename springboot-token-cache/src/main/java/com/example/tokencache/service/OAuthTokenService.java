package com.example.tokencache.service;

import com.example.tokencache.client.KeycloakTokenClient;
import com.example.tokencache.config.KeycloakTenantsProperties;
import com.example.tokencache.config.TokenCacheProperties;
import com.example.tokencache.dto.TokenRequest;
import com.example.tokencache.dto.TokenResult;
import com.example.tokencache.exception.UnknownTenantException;
import com.example.tokencache.model.CachedToken;
import com.example.tokencache.model.TokenResponse;
import com.github.benmanes.caffeine.cache.Cache;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OAuthTokenService {

  private static final Logger log = LoggerFactory.getLogger(OAuthTokenService.class);

  private final Cache<String, CachedToken> cache;
  private final KeycloakTenantsProperties tenantsProperties;
  private final TokenCacheProperties cacheProperties;
  private final KeycloakTokenClient tokenClient;
  private final Clock clock;
  private final ReentrantLock[] locks;

  public OAuthTokenService(
      Cache<String, CachedToken> cache,
      KeycloakTenantsProperties tenantsProperties,
      TokenCacheProperties cacheProperties,
      KeycloakTokenClient tokenClient,
      Clock clock
  ) {
    this.cache = cache;
    this.tenantsProperties = tenantsProperties;
    this.cacheProperties = cacheProperties;
    this.tokenClient = tokenClient;
    this.clock = clock;
    this.locks = initLocks(64);
  }

  public TokenResult getToken(TokenRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    KeycloakTenantsProperties.TenantConfig tenant = tenantsProperties.getTenants()
        .get(request.tenantId());
    if (tenant == null) {
      throw new UnknownTenantException(request.tenantId());
    }

    String tokenUrl = tenant.getTokenUrl();
    String scope = resolveScope(tenant);
    String cacheKey = buildCacheKey(request.tenantId(), tokenUrl, request.clientId());

    long now = clock.millis();
    long skewMillis = cacheProperties.getSkewSeconds() * 1000L;

    CachedToken cachedToken = cache.getIfPresent(cacheKey);
    if (isValid(cachedToken, now, skewMillis)) {
      log.info("token cacheHit=true tenantId={} clientId={}", request.tenantId(), request.clientId());
      return toResult(cachedToken, true, request);
    }

    ReentrantLock lock = locks[Math.abs(cacheKey.hashCode()) % locks.length];
    lock.lock();
    try {
      now = clock.millis();
      cachedToken = cache.getIfPresent(cacheKey);
      if (isValid(cachedToken, now, skewMillis)) {
        log.info("token cacheHit=true tenantId={} clientId={}", request.tenantId(), request.clientId());
        return toResult(cachedToken, true, request);
      }

      TokenResponse tokenResponse = tokenClient.fetchToken(
          tokenUrl,
          scope,
          request.clientId(),
          request.clientSecret()
      );

      long issuedAt = clock.millis();
      long expiresAt = issuedAt + (tokenResponse.expires_in() * 1000L);

      CachedToken refreshed = new CachedToken(
          tokenResponse.access_token(),
          tokenResponse.token_type(),
          expiresAt
      );
      cache.put(cacheKey, refreshed);
      log.info("token cacheRefresh=true tenantId={} clientId={}", request.tenantId(), request.clientId());
      return toResult(refreshed, false, request);
    } finally {
      lock.unlock();
    }
  }

  private boolean isValid(CachedToken token, long now, long skewMillis) {
    return token != null && (now + skewMillis) < token.expiresAtEpochMillis();
  }

  private String resolveScope(KeycloakTenantsProperties.TenantConfig tenant) {
    String scope = tenant.getScope();
    if (scope == null || scope.isBlank()) {
      scope = tenantsProperties.getScope();
    }
    return scope == null ? "" : scope;
  }

  private String buildCacheKey(String tenantId, String tokenUrl, String clientId) {
    return tenantId + "|" + tokenUrl + "|" + clientId;
  }

  private ReentrantLock[] initLocks(int size) {
    ReentrantLock[] array = new ReentrantLock[size];
    for (int i = 0; i < size; i++) {
      array[i] = new ReentrantLock();
    }
    return array;
  }

  private TokenResult toResult(CachedToken token, boolean cached, TokenRequest request) {
    return new TokenResult(
        token.accessToken(),
        token.tokenType(),
        token.expiresAtEpochMillis(),
        cached,
        request.tenantId(),
        request.clientId()
    );
  }
}
