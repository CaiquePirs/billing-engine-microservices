package com.billing.payment.events.data;

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
        String interval,
        String stripePriceId,
        Boolean active
) {
}
