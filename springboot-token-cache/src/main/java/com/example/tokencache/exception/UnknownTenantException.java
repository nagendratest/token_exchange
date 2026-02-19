package com.example.tokencache.exception;

public class UnknownTenantException extends RuntimeException {

  public UnknownTenantException(String tenantId) {
    super("Unknown tenantId: " + tenantId);
  }
}
