package de.telefonica.apigw.tokengenerator.dto;

import jakarta.validation.constraints.NotBlank;

public record EOrchestratorTokenRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String tenantId
) {
}
