package com.billing.subscriptions.events.data;

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
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        LocalDateTime processedAt) {
}
