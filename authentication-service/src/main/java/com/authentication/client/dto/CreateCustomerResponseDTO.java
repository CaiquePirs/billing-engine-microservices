package com.authentication.client.dto;

import java.util.UUID;

public record CreateCustomerResponseDTO(UUID customerId, String email) {
}
