package com.example.tokencache.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
    @NotBlank String tenantId,
    @NotBlank String clientId,
    @NotBlank String clientSecret
) {
}
