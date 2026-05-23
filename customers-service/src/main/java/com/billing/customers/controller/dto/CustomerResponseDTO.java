package com.billing.customers.controller.dto;

import java.time.LocalDate;
import java.util.UUID;
import com.billing.customers.model.enums.CustomerStatus;
import lombok.Builder;

@Builder
public record CustomerResponseDTO(
    UUID id,
    String name,
    String lastName,
    String email,
    String phone,
    String taxNumber,
    Integer age,
    LocalDate dateOfBirth,
    AddressResponseDTO address,
    CustomerStatus customerStatus,
    String stripeCustomerId) {}
