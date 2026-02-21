package com.example.tokencache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

@Configuration
public class AuthCacheConfig {

  @Bean(name = "eorchestratorCache")
  public Cache<String, ResponseEntity<String>> eorchestratorCache(AuthCacheProperties properties) {
    return Caffeine.newBuilder()
        .maximumSize(properties.getMaxSize())
        .expireAfterWrite(properties.getEorchestratorTtlSeconds(), TimeUnit.SECONDS)
        .build();
  }

  @Bean(name = "enmCache")
  public Cache<String, ResponseEntity<String>> enmCache(AuthCacheProperties properties) {
    return Caffeine.newBuilder()
        .maximumSize(properties.getMaxSize())
        .expireAfterWrite(properties.getEnmTtlSeconds(), TimeUnit.SECONDS)
        .build();
  }
}
