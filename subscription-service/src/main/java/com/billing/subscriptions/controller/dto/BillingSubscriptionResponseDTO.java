package com.billing.subscriptions.controller.dto;

import com.billing.subscriptions.model.enums.SubscriptionStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record BillingSubscriptionResponseDTO(
        UUID id,
        UUID customerId,
        String stripeSubscriptionId,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        SubscriptionStatus subscriptionStatus,
        PlanResponseDTO plan) {
}
