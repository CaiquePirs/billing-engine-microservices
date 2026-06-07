package com.billing.payment.events.data;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SubscriptionPaymentEvent(
        UUID paymentId,
        UUID subscriptionId,
        String paymentStatus,
        LocalDateTime processedAt) {
}
