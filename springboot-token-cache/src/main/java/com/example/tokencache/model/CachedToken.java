package com.example.tokencache.model;

public record CachedToken(
    String accessToken,
    String tokenType,
    long expiresAtEpochMillis
) {
}
