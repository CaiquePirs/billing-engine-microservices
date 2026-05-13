package com.authentication.controller.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record LoginResponseDTO(
        String access_token,
        String token_type,
        Instant expires_in
) {
}
