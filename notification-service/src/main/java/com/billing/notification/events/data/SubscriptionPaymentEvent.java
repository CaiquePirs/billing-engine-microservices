package com.billing.notification.events.data;

import com.billing.notification.model.SubscriptionNotificationEvent;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SubscriptionPaymentEvent(
        UUID paymentId,
        UUID subscriptionId,
        String paymentStatus,
        String subscriptionStatus,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        PlanResponseDTO plan,
        CustomerClientResponse customer) implements SubscriptionNotificationEvent {
}