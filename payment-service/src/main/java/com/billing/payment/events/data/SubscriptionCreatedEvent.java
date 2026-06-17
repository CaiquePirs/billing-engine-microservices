package com.billing.payment.events.data;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionCreatedEvent(
        UUID subscriptionId,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        String subscriptionStatus,
        String paymentMethodId,
        CustomerClientResponse customer,
        PlanResponseDTO plan) {
}

