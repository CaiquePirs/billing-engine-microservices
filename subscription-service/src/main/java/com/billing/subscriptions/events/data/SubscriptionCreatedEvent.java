package com.billing.subscriptions.events.data;

import com.billing.subscriptions.controller.dto.PlanResponseDTO;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record SubscriptionCreatedEvent(
        UUID id,
        UUID customerId,
        PlanResponseDTO planResponseDTO,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        SubscriptionStatus subscriptionStatus){
}
