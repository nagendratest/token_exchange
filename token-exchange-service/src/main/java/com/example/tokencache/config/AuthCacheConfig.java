package com.example.tokencache.config;

import com.example.tokencache.cache.InMemoryCache;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

@Configuration
public class AuthCacheConfig {

  @Bean(name = "eorchestratorCache")
  public InMemoryCache<String, ResponseEntity<String>> eorchestratorCache(AuthCacheProperties properties) {
    return new InMemoryCache<>(
        properties.getMaxSize(),
        Duration.ofSeconds(properties.getEorchestratorTtlSeconds())
    );
  }

  @Bean(name = "enmCache")
  public InMemoryCache<String, ResponseEntity<String>> enmCache(AuthCacheProperties properties) {
    return new InMemoryCache<>(
        properties.getMaxSize(),
        Duration.ofSeconds(properties.getEnmTtlSeconds())
    );
  }
}
