package com.authentication.client.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAddressRequestDTO(
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
        String eircode) {
}
