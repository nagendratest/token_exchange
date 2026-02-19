package com.example.tokencache.service;

import com.example.tokencache.client.KeycloakTokenClient;
import com.example.tokencache.config.KeycloakTenantsProperties;
import com.example.tokencache.config.TokenCacheProperties;
import com.example.tokencache.dto.TokenRequest;
import com.example.tokencache.dto.TokenResult;
import com.example.tokencache.model.CachedToken;
import com.example.tokencache.model.TokenResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OAuthTokenServiceTest {

  @Test
  void cacheHitReturnsCachedResultAndSingleKeycloakCall() {
    MutableClock clock = MutableClock.of(Instant.ofEpochMilli(0));
    TokenCacheProperties cacheProperties = new TokenCacheProperties();
    cacheProperties.setSkewSeconds(30);
    cacheProperties.setMaxSize(1000);

    KeycloakTenantsProperties tenants = new KeycloakTenantsProperties();
    KeycloakTenantsProperties.TenantConfig tenant = new KeycloakTenantsProperties.TenantConfig();
    tenant.setTokenUrl("http://keycloak:8080/realms/demo/protocol/openid-connect/token");
    tenants.getTenants().put("ECM", tenant);

    Cache<String, CachedToken> cache = Caffeine.newBuilder().maximumSize(1000).build();
    KeycloakTokenClient client = Mockito.mock(KeycloakTokenClient.class);

    TokenResponse response = new TokenResponse("token-1", 3600, "Bearer");
    Mockito.when(client.fetchToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(response);

    OAuthTokenService service = new OAuthTokenService(cache, tenants, cacheProperties, client, clock);

    TokenRequest request = new TokenRequest("ECM", "token-cache-client", "secret");
    TokenResult first = service.getToken(request);
    TokenResult second = service.getToken(request);

    Assertions.assertFalse(first.cached());
    Assertions.assertTrue(second.cached());
    Mockito.verify(client, Mockito.times(1))
        .fetchToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
  }

  @Test
  void refreshOnExpiryUsesClockSkew() {
    MutableClock clock = MutableClock.of(Instant.ofEpochMilli(0));
    TokenCacheProperties cacheProperties = new TokenCacheProperties();
    cacheProperties.setSkewSeconds(30);
    cacheProperties.setMaxSize(1000);

    KeycloakTenantsProperties tenants = new KeycloakTenantsProperties();
    KeycloakTenantsProperties.TenantConfig tenant = new KeycloakTenantsProperties.TenantConfig();
    tenant.setTokenUrl("http://keycloak:8080/realms/demo/protocol/openid-connect/token");
    tenants.getTenants().put("ECM", tenant);

    Cache<String, CachedToken> cache = Caffeine.newBuilder().maximumSize(1000).build();
    KeycloakTokenClient client = Mockito.mock(KeycloakTokenClient.class);

    TokenResponse response1 = new TokenResponse("token-1", 60, "Bearer");
    TokenResponse response2 = new TokenResponse("token-2", 60, "Bearer");
    Mockito.when(client.fetchToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(response1, response2);

    OAuthTokenService service = new OAuthTokenService(cache, tenants, cacheProperties, client, clock);

    TokenRequest request = new TokenRequest("ECM", "token-cache-client", "secret");
    TokenResult first = service.getToken(request);
    clock.advanceSeconds(40);
    TokenResult second = service.getToken(request);

    Assertions.assertFalse(first.cached());
    Assertions.assertFalse(second.cached());
    Assertions.assertNotEquals(first.accessToken(), second.accessToken());
    Mockito.verify(client, Mockito.times(2))
        .fetchToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
  }

  @Test
  void concurrentRequestsSingleFlight() throws Exception {
    MutableClock clock = MutableClock.of(Instant.ofEpochMilli(0));
    TokenCacheProperties cacheProperties = new TokenCacheProperties();
    cacheProperties.setSkewSeconds(30);
    cacheProperties.setMaxSize(1000);

    KeycloakTenantsProperties tenants = new KeycloakTenantsProperties();
    KeycloakTenantsProperties.TenantConfig tenant = new KeycloakTenantsProperties.TenantConfig();
    tenant.setTokenUrl("http://keycloak:8080/realms/demo/protocol/openid-connect/token");
    tenants.getTenants().put("ECM", tenant);

    Cache<String, CachedToken> cache = Caffeine.newBuilder().maximumSize(1000).build();
    KeycloakTokenClient client = Mockito.mock(KeycloakTokenClient.class);

    TokenResponse response = new TokenResponse("token-1", 3600, "Bearer");
    Mockito.when(client.fetchToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenAnswer(invocation -> {
          Thread.sleep(100);
          return response;
        });

    OAuthTokenService service = new OAuthTokenService(cache, tenants, cacheProperties, client, clock);

    TokenRequest request = new TokenRequest("ECM", "token-cache-client", "secret");
    int threads = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threads);
    List<TokenResult> results = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      executor.submit(() -> {
        try {
          start.await();
          TokenResult result = service.getToken(request);
          synchronized (results) {
            results.add(result);
          }
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      });
    }

    start.countDown();
    boolean completed = done.await(5, TimeUnit.SECONDS);
    executor.shutdownNow();

    Assertions.assertTrue(completed, "Threads did not complete in time");
    Assertions.assertEquals(threads, results.size());
    Mockito.verify(client, Mockito.times(1))
        .fetchToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
  }
}
