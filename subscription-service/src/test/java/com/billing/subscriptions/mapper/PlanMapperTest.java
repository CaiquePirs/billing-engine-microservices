package com.billing.subscriptions.mapper;

import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.controller.dto.PlanResponseDTO;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.model.enums.IntervalPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlanMapperTest {

    private final PlanMapper planMapper = new PlanMapper();

    @Test
    void toEntity_shouldMapAllFields_whenRequestIsValid() {
        PlanRequestDTO request = new PlanRequestDTO("Premium", "Premium monthly plan",
                new BigDecimal("99.00"), "EUR", IntervalPlan.MONTHLY);

        Plan result = planMapper.toEntity(request);

        assertThat(result.getName()).isEqualTo("Premium");
        assertThat(result.getDescription()).isEqualTo("Premium monthly plan");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("99.00"));
        assertThat(result.getCurrency()).isEqualTo("EUR");
        assertThat(result.getInterval()).isEqualTo(IntervalPlan.MONTHLY);
        assertThat(result.getActive()).isTrue();
        assertThat(result.getAuditLog()).isNotNull();
    }

    @Test
    void toEntity_shouldSetActiveTrue_whenCreatingNewPlan() {
        PlanRequestDTO request = new PlanRequestDTO("Basic", "Basic plan",
                new BigDecimal("19.99"), "USD", IntervalPlan.YEARLY);

        Plan result = planMapper.toEntity(request);

        assertThat(result.getActive()).isTrue();
    }

    @Test
    void toResponse_shouldMapAllFields_whenPlanIsValid() {
        UUID planId = UUID.randomUUID();
        Plan plan = Plan.builder()
                .id(planId)
                .name("Premium")
                .description("Premium plan")
                .price(new BigDecimal("99.00"))
                .currency("EUR")
                .interval(IntervalPlan.MONTHLY)
                .stripePriceId("price_abc123")
                .active(true)
                .createdBy(UUID.randomUUID())
                .build();

        PlanResponseDTO result = planMapper.toResponse(plan);

        assertThat(result.id()).isEqualTo(planId);
        assertThat(result.name()).isEqualTo("Premium");
        assertThat(result.description()).isEqualTo("Premium plan");
        assertThat(result.price()).isEqualTo(new BigDecimal("99.00"));
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.interval()).isEqualTo("MONTHLY");
        assertThat(result.stripePriceId()).isEqualTo("price_abc123");
        assertThat(result.active()).isTrue();
    }
}
