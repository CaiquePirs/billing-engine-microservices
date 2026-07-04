package com.billing.subscriptions.mapper;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.dto.BillingSubscriptionResponseDTO;
import com.billing.subscriptions.controller.dto.PlanResponseDTO;
import com.billing.subscriptions.events.data.SubscriptionCreatedEvent;
import com.billing.subscriptions.model.AuditLog;
import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.model.enums.IntervalPlan;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingSubscriptionMapperTest {

    @Mock private PlanMapper planMapper;

    @InjectMocks
    private BillingSubscriptionMapper billingSubscriptionMapper;

    private Plan buildPlan(IntervalPlan interval) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("Premium")
                .description("Premium plan")
                .price(new BigDecimal("99.00"))
                .currency("EUR")
                .interval(interval)
                .stripePriceId("price_abc123")
                .active(true)
                .createdBy(UUID.randomUUID())
                .auditLog(new AuditLog())
                .build();
    }

    private PlanResponseDTO buildPlanResponse() {
        return PlanResponseDTO.builder()
                .id(UUID.randomUUID())
                .name("Premium")
                .description("Premium plan")
                .price(new BigDecimal("99.00"))
                .currency("EUR")
                .interval("MONTHLY")
                .active(true)
                .build();
    }

    private CustomerClientResponse buildCustomer() {
        return new CustomerClientResponse(UUID.randomUUID(), "John", "Doe",
                "john@example.com", "+35312345678", "123456789", 34,
                LocalDate.of(1990, 1, 1), null, "ACTIVE", "cus_abc123");
    }

    @Test
    void toEntity_shouldCreatePendingSubscription_withMonthlyPeriodEnd() {
        Plan plan = buildPlan(IntervalPlan.MONTHLY);
        UUID customerId = UUID.randomUUID();

        BillingSubscription result = billingSubscriptionMapper.toEntity(plan, customerId);

        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getPlan()).isEqualTo(plan);
        assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(result.getCurrentPeriodStart()).isEqualTo(LocalDate.now());
        assertThat(result.getCurrentPeriodEnd()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void toEntity_shouldCreatePendingSubscription_withYearlyPeriodEnd() {
        Plan plan = buildPlan(IntervalPlan.YEARLY);
        UUID customerId = UUID.randomUUID();

        BillingSubscription result = billingSubscriptionMapper.toEntity(plan, customerId);

        assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(result.getCurrentPeriodEnd()).isEqualTo(LocalDate.now().plusYears(1));
    }

    @Test
    void toResponse_shouldMapAllFields_whenSubscriptionIsValid() {
        Plan plan = buildPlan(IntervalPlan.MONTHLY);
        PlanResponseDTO planResponse = buildPlanResponse();
        UUID subscriptionId = UUID.randomUUID();

        BillingSubscription subscription = BillingSubscription.builder()
                .id(subscriptionId)
                .customerId(UUID.randomUUID())
                .plan(plan)
                .stripeSubscriptionId("sub_abc123")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .auditLog(new AuditLog())
                .build();

        when(planMapper.toResponse(plan)).thenReturn(planResponse);

        BillingSubscriptionResponseDTO result = billingSubscriptionMapper.toResponse(subscription);

        assertThat(result.id()).isEqualTo(subscriptionId);
        assertThat(result.stripeSubscriptionId()).isEqualTo("sub_abc123");
        assertThat(result.subscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.plan()).isEqualTo(planResponse);
    }

    @Test
    void mapToSubscriptionCreatedEvent_shouldMapAllFields_whenDataIsValid() {
        Plan plan = buildPlan(IntervalPlan.MONTHLY);
        PlanResponseDTO planResponse = buildPlanResponse();
        CustomerClientResponse customer = buildCustomer();
        UUID subscriptionId = UUID.randomUUID();

        BillingSubscription subscription = BillingSubscription.builder()
                .id(subscriptionId)
                .customerId(customer.id())
                .plan(plan)
                .subscriptionStatus(SubscriptionStatus.PENDING)
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .auditLog(new AuditLog())
                .build();

        when(planMapper.toResponse(plan)).thenReturn(planResponse);

        SubscriptionCreatedEvent result = billingSubscriptionMapper.mapToSubscriptionCreatedEvent(subscription, customer, "pm_test123");

        assertThat(result.subscriptionId()).isEqualTo(subscriptionId);
        assertThat(result.paymentMethodId()).isEqualTo("pm_test123");
        assertThat(result.customer()).isEqualTo(customer);
        assertThat(result.plan().name()).isEqualTo(planResponse.name());
        assertThat(result.plan().currency()).isEqualTo(planResponse.currency());
        assertThat(result.plan().interval()).isEqualTo(planResponse.interval());
    }

    @Test
    void mapToSubscriptionCreatedEvent_shouldConvertPlanPriceToCents_whenDataIsValid() {
        Plan plan = buildPlan(IntervalPlan.MONTHLY);
        PlanResponseDTO planResponse = buildPlanResponse();
        CustomerClientResponse customer = buildCustomer();

        BillingSubscription subscription = BillingSubscription.builder()
                .id(UUID.randomUUID())
                .customerId(customer.id())
                .plan(plan)
                .subscriptionStatus(SubscriptionStatus.PENDING)
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .auditLog(new AuditLog())
                .build();

        when(planMapper.toResponse(plan)).thenReturn(planResponse);

        SubscriptionCreatedEvent result = billingSubscriptionMapper.mapToSubscriptionCreatedEvent(subscription, customer, "pm_test123");

        assertThat(result.plan().price()).isEqualByComparingTo(new BigDecimal("9900"));
    }

    @Test
    void toPeriodPlan_shouldReturnMonthlyDate_whenIntervalIsMonthly() {
        LocalDate result = billingSubscriptionMapper.toPeriodPlan(IntervalPlan.MONTHLY);

        assertThat(result).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void toPeriodPlan_shouldReturnYearlyDate_whenIntervalIsYearly() {
        LocalDate result = billingSubscriptionMapper.toPeriodPlan(IntervalPlan.YEARLY);

        assertThat(result).isEqualTo(LocalDate.now().plusYears(1));
    }
}
