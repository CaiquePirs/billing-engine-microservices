package com.billing.customers.controller.dto;

import com.billing.customers.model.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CustomerRequestDTO(
    @NotBlank(message = "Name is required")
    String name,

    @NotBlank(message = "Last name is required")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email,

    @NotBlank(message = "Phone is required")
    String phone,

    @NotBlank(message = "Tax number is required")
    String taxNumber,

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    LocalDate dateOfBirth,

    @NotNull(message = "Customer role is required")
    Role role,

    @NotNull(message = "Address is required")
    @Valid
    AddressRequestDTO address
) {}
