package com.authentication.controller.dto;

import com.authentication.model.enums.Role;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AuthenticationResponseDTO(
        UUID authenticationId,
        UUID customerId,
        String email,
        Role role) {
}
