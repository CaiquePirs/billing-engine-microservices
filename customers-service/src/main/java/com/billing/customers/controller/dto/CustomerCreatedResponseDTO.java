package com.billing.customers.controller.dto;

import java.util.UUID;

public record CustomerCreatedResponseDTO(UUID customerId, String email) {
}
