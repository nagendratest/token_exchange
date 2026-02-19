package com.example.tokencache.dto;

public record TokenResult(
    String accessToken,
    String tokenType,
    long expiresAtEpochMillis,
    boolean cached,
    String tenantId,
    String clientId
) {
}
