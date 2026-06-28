package com.billing.subscriptions.validator;

import com.billing.subscriptions.controller.advice.exception.DuplicateSubscriptionException;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import com.billing.subscriptions.repository.BillingSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionValidator {

    private final BillingSubscriptionRepository billingSubscriptionRepository;

    private static final List<SubscriptionStatus> ACTIVE_STATUSES = List.of(
            SubscriptionStatus.PENDING,
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.TRIALING
    );

    public void validateSubscription(UUID planId, UUID customerId) {
        boolean alreadySubscribed = billingSubscriptionRepository.existsByCustomerIdAndPlan_IdAndSubscriptionStatusIn(
                customerId, planId, ACTIVE_STATUSES
        );
        if (alreadySubscribed) {
            log.warn("Customer ID: {} already has an active subscription for plan ID: {}", customerId, planId);
            throw new DuplicateSubscriptionException("Customer already has an active subscription for plan: " + planId);
        }
    }
}
