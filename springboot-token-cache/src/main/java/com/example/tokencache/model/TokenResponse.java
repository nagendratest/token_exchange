package com.example.tokencache.model;

public record TokenResponse(
    String access_token,
    long expires_in,
    String token_type
) {
}
