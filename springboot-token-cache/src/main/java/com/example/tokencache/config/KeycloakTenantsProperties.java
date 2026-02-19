package com.example.tokencache.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public class KeycloakTenantsProperties {

  private String scope = "";
  private Map<String, TenantConfig> tenants = new HashMap<>();

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public Map<String, TenantConfig> getTenants() {
    return tenants;
  }

  public void setTenants(Map<String, TenantConfig> tenants) {
    this.tenants = tenants;
  }

  public static class TenantConfig {
    private String tokenUrl;
    private String scope;

    public String getTokenUrl() {
      return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
      this.tokenUrl = tokenUrl;
    }

    public String getScope() {
      return scope;
    }

    public void setScope(String scope) {
      this.scope = scope;
    }
  }
}
