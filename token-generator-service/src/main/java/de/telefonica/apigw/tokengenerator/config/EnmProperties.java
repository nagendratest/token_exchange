package de.telefonica.apigw.tokengenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enm")
public class EnmProperties {

  private String baseUrl;
  private String path;
  private String resolveHost;
  private Integer resolvePort;
  private String resolveAddress;
  private boolean insecureTls;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getResolveHost() {
    return resolveHost;
  }

  public void setResolveHost(String resolveHost) {
    this.resolveHost = resolveHost;
  }

  public Integer getResolvePort() {
    return resolvePort;
  }

  public void setResolvePort(Integer resolvePort) {
    this.resolvePort = resolvePort;
  }

  public String getResolveAddress() {
    return resolveAddress;
  }

  public void setResolveAddress(String resolveAddress) {
    this.resolveAddress = resolveAddress;
  }

  public boolean isInsecureTls() {
    return insecureTls;
  }

  public void setInsecureTls(boolean insecureTls) {
    this.insecureTls = insecureTls;
  }
}
