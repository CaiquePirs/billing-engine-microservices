package com.billing.notification.events.data;

import com.billing.notification.model.SubscriptionNotificationEvent;
import lombok.Builder;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record InvoiceCreatedEvent(
        UUID invoiceId,
        String s3Key,
        UUID subscriptionId,
        String status,
        String paymentStatus,
        String subscriptionStatus,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        PlanResponseDTO plan,
        CustomerClientResponse customer) implements SubscriptionNotificationEvent {
}
