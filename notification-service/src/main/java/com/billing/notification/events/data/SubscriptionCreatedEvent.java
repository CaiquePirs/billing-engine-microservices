package com.billing.notification.events.data;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionCreatedEvent(
        UUID id,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        String subscriptionStatus,
        String paymentMethodId,
        CustomerClientResponse customer,
        PlanResponseDTO plan) {
}
