package de.telefonica.apigw.tokengenerator.service;

import de.telefonica.apigw.tokengenerator.cache.InMemoryCache;
import de.telefonica.apigw.tokengenerator.config.AuthCacheProperties;
import de.telefonica.apigw.tokengenerator.config.EnmProperties;
import de.telefonica.apigw.tokengenerator.dto.EnmLoginRequest;
import de.telefonica.apigw.tokengenerator.dto.EnmLoginResponse;
import de.telefonica.apigw.tokengenerator.exception.TokenAcquisitionException;
import java.net.InetAddress;
import java.time.Duration;
import org.apache.hc.client5.http.DnsResolver;
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
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EnmServiceTest {

  @Test
  void loginCachesSuccessfulResponse() {
    EnmProperties properties = new EnmProperties();
    properties.setBaseUrl("https://te.testmob.de");
    properties.setPath("/login");
    AuthCacheProperties cacheProperties = new AuthCacheProperties();
    cacheProperties.setEnmTtlSeconds(1800);

    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    InMemoryCache<String, ResponseEntity<EnmLoginResponse>> cache =
        new InMemoryCache<>(100, Duration.ofSeconds(1800));

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add(
        HttpHeaders.SET_COOKIE,
        "iPlanetDirectoryPro=token-123;Version=0;Path=/;Secure;HttpOnly"
    );

    server.expect(ExpectedCount.once(), requestTo("https://te.testmob.de/login"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("TenantId", "PacketCore5G"))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString("application/x-www-form-urlencoded")))
        .andExpect(content().string("IDToken1=test&IDToken2=asdfasdfdf%401234"))
        .andRespond(withSuccess("login-ok", MediaType.TEXT_PLAIN).headers(responseHeaders));

    EnmService service = new EnmService(builder, properties, cache, cacheProperties);
    EnmLoginRequest request = new EnmLoginRequest("test", "asdfasdfdf@1234", "PacketCore5G");

    ResponseEntity<EnmLoginResponse> first = service.login(request);
    ResponseEntity<EnmLoginResponse> second = service.login(request);

    assertThat(first.getBody()).isNotNull();
    assertThat(second.getBody()).isNotNull();
    assertThat(first.getBody().access_token()).isEqualTo("token-123");
    assertThat(second.getBody().access_token()).isEqualTo("token-123");

    server.verify();
  }

  @Test
  void errorResponsesAreNotCached() {
    EnmProperties properties = new EnmProperties();
    properties.setBaseUrl("https://te.testmob.de");
    properties.setPath("/login");
    AuthCacheProperties cacheProperties = new AuthCacheProperties();
    cacheProperties.setEnmTtlSeconds(1800);

    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    InMemoryCache<String, ResponseEntity<EnmLoginResponse>> cache =
        new InMemoryCache<>(100, Duration.ofSeconds(1800));

    server.expect(ExpectedCount.times(2), requestTo("https://te.testmob.de/login"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    EnmService service = new EnmService(builder, properties, cache, cacheProperties);
    EnmLoginRequest request = new EnmLoginRequest("test", "asdfasdfdf@1234", "PacketCore5G");

    assertThrows(TokenAcquisitionException.class, () -> service.login(request));
    assertThrows(TokenAcquisitionException.class, () -> service.login(request));

    server.verify();
  }

  @Test
  void dnsResolverOverridesConfiguredHost() throws Exception {
    EnmProperties properties = new EnmProperties();
    properties.setBaseUrl("https://te.testmob.de");
    properties.setResolveHost("te.testmob.de");
    properties.setResolvePort(443);
    properties.setResolveAddress("10.118.123.174");

    DnsResolver resolver = EnmService.buildDnsResolver(properties);
    InetAddress[] addresses = resolver.resolve("te.testmob.de");

    assertThat(addresses).hasSize(1);
    assertThat(addresses[0].getHostAddress()).isEqualTo("10.118.123.174");
    assertThat(EnmService.effectivePort(properties.getBaseUrl())).isEqualTo(443);
  }

  @Test
  void rejectsResolvePortThatDoesNotMatchBaseUrl() {
    EnmProperties properties = new EnmProperties();
    properties.setBaseUrl("https://te.testmob.de");
    properties.setPath("/login");
    properties.setResolveHost("te.testmob.de");
    properties.setResolvePort(8443);
    properties.setResolveAddress("10.118.123.174");
    properties.setInsecureTls(true);
    AuthCacheProperties cacheProperties = new AuthCacheProperties();
    cacheProperties.setEnmTtlSeconds(1800);
    InMemoryCache<String, ResponseEntity<EnmLoginResponse>> cache =
        new InMemoryCache<>(100, Duration.ofSeconds(1800));

    assertThrows(
        IllegalStateException.class,
        () -> new EnmService(RestClient.builder(), properties, cache, cacheProperties)
    );
  }
}
