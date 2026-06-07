package com.billing.payment.events.data;

import java.math.BigDecimal;
import java.util.UUID;

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
