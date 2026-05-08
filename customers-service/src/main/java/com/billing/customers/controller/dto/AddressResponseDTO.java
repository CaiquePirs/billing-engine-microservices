package com.billing.customers.controller.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AddressResponseDTO(
    UUID id,
    String street,
    String number,
    String city,
    String state,
    String county,
    String eircode
) {}

