package com.example.tokencache.service;

import com.example.tokencache.config.EnmProperties;
import com.example.tokencache.config.AuthCacheProperties;
import com.example.tokencache.dto.EnmLoginRequest;
import com.example.tokencache.exception.TokenAcquisitionException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EnmServiceTest {

  @Test
  void loginCachesSuccessfulResponse() {
    EnmProperties properties = new EnmProperties();
    properties.setBaseUrl("https://localhost:443");
    properties.setPath("/login");
    AuthCacheProperties cacheProperties = new AuthCacheProperties();
    cacheProperties.setEnmTtlSeconds(1800);

    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    Cache<String, ResponseEntity<String>> cache = Caffeine.newBuilder().maximumSize(100).build();

    server.expect(ExpectedCount.once(), requestTo("https://localhost:443/login"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Cookie", "IDToken1=admin; IDToken2=secret"))
        .andRespond(withSuccess("login-ok", MediaType.TEXT_PLAIN));

    EnmService service = new EnmService(builder, properties, cache, cacheProperties);
    EnmLoginRequest request = new EnmLoginRequest("admin", "secret");

    ResponseEntity<String> first = service.login(request);
    ResponseEntity<String> second = service.login(request);

    assertThat(first.getBody()).isEqualTo("login-ok");
    assertThat(second.getBody()).isEqualTo("login-ok");

    server.verify();
  }

  @Test
  void errorResponsesAreNotCached() {
    EnmProperties properties = new EnmProperties();
    properties.setBaseUrl("https://localhost:443");
    properties.setPath("/login");
    AuthCacheProperties cacheProperties = new AuthCacheProperties();
    cacheProperties.setEnmTtlSeconds(1800);

    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    Cache<String, ResponseEntity<String>> cache = Caffeine.newBuilder().maximumSize(100).build();

    server.expect(ExpectedCount.times(2), requestTo("https://localhost:443/login"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    EnmService service = new EnmService(builder, properties, cache, cacheProperties);
    EnmLoginRequest request = new EnmLoginRequest("admin", "secret");

    assertThrows(TokenAcquisitionException.class, () -> service.login(request));
    assertThrows(TokenAcquisitionException.class, () -> service.login(request));

    server.verify();
  }
}
