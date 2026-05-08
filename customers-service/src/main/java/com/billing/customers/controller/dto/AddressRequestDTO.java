package com.billing.customers.controller.dto;

import jakarta.validation.constraints.*;

public record AddressRequestDTO(
    @NotBlank(message = "Street is required")
    String street,

    @NotBlank(message = "Number is required")
    String number,

    @NotBlank(message = "City is required")
    String city,

    @NotBlank(message = "State is required")
    String state,

    @NotBlank(message = "County is required")
    String county,

    @NotBlank(message = "Eircode is required")
    String eircode
) {}
