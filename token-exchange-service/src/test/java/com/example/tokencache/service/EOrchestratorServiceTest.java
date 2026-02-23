package com.example.tokencache.service;

import com.example.tokencache.config.EOrchestratorProperties;
import com.example.tokencache.config.AuthCacheProperties;
import com.example.tokencache.cache.InMemoryCache;
import com.example.tokencache.dto.EOrchestratorTokenRequest;
import com.example.tokencache.exception.TokenAcquisitionException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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

class EOrchestratorServiceTest {

  @Test
  void fetchTokenCachesSuccessfulResponse() {
    EOrchestratorProperties properties = new EOrchestratorProperties();
    properties.setBaseUrl("https://localhost:443");
    properties.setPath("/ecm_service/tokens");
    AuthCacheProperties cacheProperties = new AuthCacheProperties();
    cacheProperties.setEorchestratorTtlSeconds(1800);

    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    InMemoryCache<String, ResponseEntity<String>> cache =
        new InMemoryCache<>(100, Duration.ofSeconds(1800));

    String raw = "user1:pass1";
    String encoded = Base64.getEncoder()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));

    server.expect(ExpectedCount.once(), requestTo("https://localhost:443/ecm_service/tokens"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic " + encoded))
        .andExpect(header("TenantId", "TENANT"))
        .andRespond(withSuccess("token-1", MediaType.TEXT_PLAIN));

    EOrchestratorService service = new EOrchestratorService(builder, properties, cache, cacheProperties);
    EOrchestratorTokenRequest request = new EOrchestratorTokenRequest("user1", "pass1", "TENANT");

    ResponseEntity<String> first = service.fetchToken(request);
    ResponseEntity<String> second = service.fetchToken(request);

    assertThat(first.getBody()).isEqualTo("token-1");
    assertThat(second.getBody()).isEqualTo("token-1");

    server.verify();
  }

  @Test
  void errorResponsesAreNotCached() {
    EOrchestratorProperties properties = new EOrchestratorProperties();
    properties.setBaseUrl("https://localhost:443");
    properties.setPath("/ecm_service/tokens");
    AuthCacheProperties cacheProperties = new AuthCacheProperties();
    cacheProperties.setEorchestratorTtlSeconds(1800);

    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    InMemoryCache<String, ResponseEntity<String>> cache =
        new InMemoryCache<>(100, Duration.ofSeconds(1800));

    server.expect(ExpectedCount.times(2), requestTo("https://localhost:443/ecm_service/tokens"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    EOrchestratorService service = new EOrchestratorService(builder, properties, cache, cacheProperties);
    EOrchestratorTokenRequest request = new EOrchestratorTokenRequest("user1", "pass1", "TENANT");

    assertThrows(TokenAcquisitionException.class, () -> service.fetchToken(request));
    assertThrows(TokenAcquisitionException.class, () -> service.fetchToken(request));

    server.verify();
  }
}
