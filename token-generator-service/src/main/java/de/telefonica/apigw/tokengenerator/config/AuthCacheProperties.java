package de.telefonica.apigw.tokengenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cache")
public class AuthCacheProperties {

  private long maxSize = 1000;
  private long eorchestratorTtlSeconds = 1800;
  private long enmTtlSeconds = 1800;

  public long getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(long maxSize) {
    this.maxSize = maxSize;
  }

  public long getEorchestratorTtlSeconds() {
    return eorchestratorTtlSeconds;
  }

  public void setEorchestratorTtlSeconds(long eorchestratorTtlSeconds) {
    this.eorchestratorTtlSeconds = eorchestratorTtlSeconds;
  }

  public long getEnmTtlSeconds() {
    return enmTtlSeconds;
  }

  public void setEnmTtlSeconds(long enmTtlSeconds) {
    this.enmTtlSeconds = enmTtlSeconds;
  }
}
