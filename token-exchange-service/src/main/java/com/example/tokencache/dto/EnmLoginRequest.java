package com.example.tokencache.dto;

import jakarta.validation.constraints.NotBlank;

public record EnmLoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {
}
