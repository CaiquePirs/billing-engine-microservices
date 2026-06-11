package com.billing.subscriptions.events.data;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.dto.PlanResponseDTO;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SubscriptionPaymentEvent(
        UUID paymentId,
        UUID subscriptionId,
        String stripeSubscriptionId,
        String paymentStatus,
        String subscriptionStatus,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        PlanResponseDTO plan,
        CustomerClientResponse customer) {
}
