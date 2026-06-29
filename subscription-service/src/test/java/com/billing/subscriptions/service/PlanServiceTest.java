package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.NotFoundException;
import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.mapper.PlanMapper;
import com.billing.subscriptions.model.AuditLog;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.model.enums.IntervalPlan;
import com.billing.subscriptions.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock private PlanRepository planRepository;
    @Mock private PlanMapper planMapper;
    @Mock private SecurityService securityService;
    @Mock private StripePlanService stripePlanService;

    @InjectMocks
    private PlanService planService;

    private Plan buildPlan(boolean active) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("Premium")
                .description("Premium monthly plan")
                .price(new BigDecimal("99.00"))
                .currency("EUR")
                .interval(IntervalPlan.MONTHLY)
                .stripePriceId("price_abc123")
                .active(active)
                .createdBy(UUID.randomUUID())
                .auditLog(new AuditLog())
                .build();
    }

    @Test
    void createPlan_shouldReturnSavedPlan_whenDataIsValid() {
        PlanRequestDTO request = new PlanRequestDTO("Premium", "Premium plan", new BigDecimal("99.00"), "EUR", IntervalPlan.MONTHLY);
        UUID adminId = UUID.randomUUID();
        Plan mappedPlan = buildPlan(true);
        mappedPlan.setStripePriceId(null);
        mappedPlan.setCreatedBy(null);

        when(planMapper.toEntity(request)).thenReturn(mappedPlan);
        when(securityService.getLoggedInAdmin()).thenReturn(adminId);
        when(stripePlanService.createStripePrice(request)).thenReturn("price_abc123");
        when(planRepository.save(mappedPlan)).thenReturn(mappedPlan);

        Plan result = planService.createPlan(request);

        assertThat(result).isNotNull();
        assertThat(result.getStripePriceId()).isEqualTo("price_abc123");
        assertThat(result.getCreatedBy()).isEqualTo(adminId);
        verify(planRepository).save(mappedPlan);
    }

    @Test
    void createPlan_shouldNotSavePlan_whenStripeServiceFails() {
        PlanRequestDTO request = new PlanRequestDTO("Premium", "Premium plan", new BigDecimal("99.00"), "EUR", IntervalPlan.MONTHLY);
        when(planMapper.toEntity(request)).thenReturn(buildPlan(true));
        when(securityService.getLoggedInAdmin()).thenReturn(UUID.randomUUID());
        when(stripePlanService.createStripePrice(request)).thenThrow(new RuntimeException("Stripe error"));

        assertThatThrownBy(() -> planService.createPlan(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stripe error");

        verify(planRepository, never()).save(any());
    }

    @Test
    void findPlanById_shouldReturnPlan_whenPlanIsActive() {
        UUID planId = UUID.randomUUID();
        Plan activePlan = buildPlan(true);
        activePlan.setId(planId);

        when(planRepository.findById(planId)).thenReturn(Optional.of(activePlan));

        Plan result = planService.findPlanById(planId);

        assertThat(result.getId()).isEqualTo(planId);
        assertThat(result.getActive()).isTrue();
    }

    @Test
    void findPlanById_shouldThrowNotFoundException_whenPlanDoesNotExist() {
        UUID planId = UUID.randomUUID();
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.findPlanById(planId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No active plan found with ID");
    }

    @Test
    void findPlanById_shouldThrowNotFoundException_whenPlanIsInactive() {
        UUID planId = UUID.randomUUID();
        Plan inactivePlan = buildPlan(false);
        inactivePlan.setId(planId);

        when(planRepository.findById(planId)).thenReturn(Optional.of(inactivePlan));

        assertThatThrownBy(() -> planService.findPlanById(planId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No active plan found with ID");
    }
}
