package com.billing.subscriptions.validator;

import com.billing.subscriptions.controller.advice.exception.DuplicateSubscriptionException;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import com.billing.subscriptions.repository.BillingSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionValidatorTest {

    @Mock private BillingSubscriptionRepository billingSubscriptionRepository;

    @InjectMocks
    private SubscriptionValidator subscriptionValidator;

    @Test
    void validateSubscription_shouldNotThrow_whenCustomerHasNoActiveSubscriptionForPlan() {
        UUID planId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(billingSubscriptionRepository.existsByCustomerIdAndPlan_IdAndSubscriptionStatusIn(
                eq(customerId), eq(planId), any(List.class))).thenReturn(false);

        assertThatCode(() -> subscriptionValidator.validateSubscription(planId, customerId))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSubscription_shouldThrowDuplicateSubscriptionException_whenCustomerAlreadySubscribed() {
        UUID planId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(billingSubscriptionRepository.existsByCustomerIdAndPlan_IdAndSubscriptionStatusIn(
                eq(customerId), eq(planId), any(List.class))).thenReturn(true);

        assertThatThrownBy(() -> subscriptionValidator.validateSubscription(planId, customerId))
                .isInstanceOf(DuplicateSubscriptionException.class)
                .hasMessageContaining("already has an active subscription for plan");
    }

    @Test
    void validateSubscription_shouldThrowDuplicateSubscriptionException_whenCustomerHasPendingSubscription() {
        UUID planId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(billingSubscriptionRepository.existsByCustomerIdAndPlan_IdAndSubscriptionStatusIn(
                eq(customerId), eq(planId), any(List.class))).thenReturn(true);

        assertThatThrownBy(() -> subscriptionValidator.validateSubscription(planId, customerId))
                .isInstanceOf(DuplicateSubscriptionException.class);
    }
}
