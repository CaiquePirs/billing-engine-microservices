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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BillingSubscriptionMapper {

    private final PlanMapper planMapper;

    public BillingSubscription toEntity(Plan plan, UUID customerId) {
        return BillingSubscription.builder()
                .customerId(customerId)
                .plan(plan)
                .subscriptionStatus(SubscriptionStatus.PENDING)
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(toPeriodPlan(plan.getInterval()))
                .auditLog(new AuditLog())
                .build();
    }

    public BillingSubscriptionResponseDTO toResponse(BillingSubscription billingSubscription){
        return BillingSubscriptionResponseDTO.builder()
                .id(billingSubscription.getId())
                .customerId(billingSubscription.getCustomerId())
                .stripeSubscriptionId(billingSubscription.getStripeSubscriptionId())
                .currentPeriodStart(billingSubscription.getCurrentPeriodStart())
                .currentPeriodEnd(billingSubscription.getCurrentPeriodEnd())
                .subscriptionStatus(billingSubscription.getSubscriptionStatus())
                .plan(planMapper.toResponse(billingSubscription.getPlan()))
                .build();
    }

    public SubscriptionCreatedEvent mapToSubscriptionCreatedEvent(BillingSubscription billingSubscription, CustomerClientResponse customer, String paymentMethodId){
        return SubscriptionCreatedEvent.builder()
                .subscriptionId(billingSubscription.getId())
                .plan(toEventPlan(billingSubscription.getPlan()))
                .currentPeriodStart(billingSubscription.getCurrentPeriodStart())
                .currentPeriodEnd(billingSubscription.getCurrentPeriodEnd())
                .subscriptionStatus(billingSubscription.getSubscriptionStatus().toString())
                .paymentMethodId(paymentMethodId)
                .customer(customer)
                .build();
    }

    // Event payload uses cents (Stripe convention, like every other price on the wire); Plan/REST API stay in whole units.
    private PlanResponseDTO toEventPlan(Plan plan) {
        PlanResponseDTO response = planMapper.toResponse(plan);
        return PlanResponseDTO.builder()
                .id(response.id())
                .name(response.name())
                .description(response.description())
                .price(response.price().movePointRight(2).setScale(0, RoundingMode.UNNECESSARY))
                .currency(response.currency())
                .interval(response.interval())
                .stripePriceId(response.stripePriceId())
                .active(response.active())
                .build();
    }

    public LocalDate toPeriodPlan(IntervalPlan intervalPlan){
        return switch (intervalPlan) {
            case MONTHLY -> LocalDate.now().plusMonths(1);
            case YEARLY -> LocalDate.now().plusYears(1);
        };
    }
}
