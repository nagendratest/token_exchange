package com.example.tokencache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "token.cache")
public class TokenCacheProperties {

  private long skewSeconds = 30;
  private long maxSize = 1000;

  public long getSkewSeconds() {
    return skewSeconds;
  }

  public void setSkewSeconds(long skewSeconds) {
    this.skewSeconds = skewSeconds;
  }

  public long getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(long maxSize) {
    this.maxSize = maxSize;
  }
}
