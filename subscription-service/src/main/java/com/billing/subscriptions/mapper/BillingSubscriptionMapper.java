package com.billing.subscriptions.mapper;

import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
import com.billing.subscriptions.controller.dto.BillingSubscriptionResponseDTO;
import com.billing.subscriptions.model.AuditLog;
import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BillingSubscriptionMapper {

    private final PlanMapper planMapper;

    public BillingSubscription toEntity(Subscription stripeSubscription, Plan plan, UUID customerId) {
        return BillingSubscription.builder()
                .customerId(customerId)
                .stripeSubscriptionId(stripeSubscription.getId())
                .plan(plan)
                .subscriptionStatus(toStripeStatus(stripeSubscription.getStatus()))
                .currentPeriodStart(toLocalDate(stripeSubscription.getCurrentPeriodStart()))
                .currentPeriodEnd(toLocalDate(stripeSubscription.getCurrentPeriodEnd()))
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

    private SubscriptionStatus toStripeStatus(String status) {
        return switch (status.toLowerCase()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            default -> SubscriptionStatus.INCOMPLETE;
        };
    }

    private LocalDate toLocalDate(Long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

}
