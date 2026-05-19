package com.billing.subscriptions.controller.dto;

import com.billing.subscriptions.model.enums.IntervalPlan;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record PlanResponseDTO(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        IntervalPlan interval,
        String stripePriceId,
        Boolean active) {
}