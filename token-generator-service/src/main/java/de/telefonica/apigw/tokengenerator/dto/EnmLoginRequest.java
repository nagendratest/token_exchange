package de.telefonica.apigw.tokengenerator.dto;

import jakarta.validation.constraints.NotBlank;

public record EnmLoginRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String tenantId
) {
}
