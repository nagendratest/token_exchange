package com.example.tokencache.config;

import com.example.tokencache.model.CachedToken;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenCacheConfig {

  @Bean
  public Cache<String, CachedToken> tokenCache(TokenCacheProperties properties) {
    return Caffeine.newBuilder()
        .maximumSize(properties.getMaxSize())
        .build();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
