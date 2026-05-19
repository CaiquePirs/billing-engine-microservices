package com.billing.subscriptions.controller.dto;

import com.billing.subscriptions.model.enums.IntervalPlan;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PlanRequestDTO(
        @NotBlank(message = "Plan name is required")
        @Size(min = 3, max = 150, message = "Plan name must be between 3 and 150 characters")
        String name,

        @NotBlank(message = "Description is required")
        @Size(min = 5, max = 500, message = "Description must be between 5 and 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Invalid price format")
        BigDecimal price,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "^[A-Z]{3}$",
                message = "Currency must follow ISO-4217 format (e.g. EUR, USD, BRL)"
        )
        String currency,

        @NotNull(message = "Interval plan is required")
        IntervalPlan interval) {
}
