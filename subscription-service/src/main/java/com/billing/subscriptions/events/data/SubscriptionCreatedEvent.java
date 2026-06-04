package com.billing.subscriptions.events.data;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.dto.PlanResponseDTO;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record SubscriptionCreatedEvent(
        UUID id,
        UUID customerId,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        String subscriptionStatus,
        String paymentMethodId,
        CustomerClientResponse customer,
        PlanResponseDTO plan){
}
