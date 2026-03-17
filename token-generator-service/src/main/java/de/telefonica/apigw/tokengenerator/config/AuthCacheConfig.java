package de.telefonica.apigw.tokengenerator.config;

import de.telefonica.apigw.tokengenerator.cache.InMemoryCache;
import de.telefonica.apigw.tokengenerator.dto.EnmLoginResponse;
import de.telefonica.apigw.tokengenerator.dto.EOrchestratorTokenResponse;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

@Configuration
public class AuthCacheConfig {

  @Bean(name = "eorchestratorCache")
  public InMemoryCache<String, ResponseEntity<EOrchestratorTokenResponse>> eorchestratorCache(
      AuthCacheProperties properties
  ) {
    return new InMemoryCache<>(
        properties.getMaxSize(),
        Duration.ofSeconds(properties.getEorchestratorTtlSeconds())
    );
  }

  @Bean(name = "enmCache")
  public InMemoryCache<String, ResponseEntity<EnmLoginResponse>> enmCache(AuthCacheProperties properties) {
    return new InMemoryCache<>(
        properties.getMaxSize(),
        Duration.ofSeconds(properties.getEnmTtlSeconds())
    );
  }
}
