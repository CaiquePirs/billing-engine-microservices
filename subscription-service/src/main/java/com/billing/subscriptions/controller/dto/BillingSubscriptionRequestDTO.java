package com.billing.subscriptions.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BillingSubscriptionRequestDTO(
        @NotNull(message = "Plan ID is required")
        UUID planId,

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotBlank(message = "Payment method ID is required")
        String paymentMethodId) {
}