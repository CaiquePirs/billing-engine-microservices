package com.billing.notification.model;

import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.PlanResponseDTO;
import lombok.Builder;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record SubscriptionEventNotification(
        UUID subscriptionId,
        String subscriptionStatus,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        String template,
        CustomerClientResponse customer,
        PlanResponseDTO plan) {
}
