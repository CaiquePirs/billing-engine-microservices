package com.authentication.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalLoginRequestDTO(

        @NotBlank(message = "ClientId is required")
        String clientId,

        @NotBlank(message = "Client Secret is required")
        String clientSecret) {
}
