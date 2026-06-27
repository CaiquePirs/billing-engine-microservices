package com.billing.payment.service;

import com.billing.payment.controller.advice.exceptions.StripeIntegrationException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.publisher.PaymentEventPublisher;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class StripeSubscriptionService {

    private final PaymentEventPublisher paymentEventPublisher;

    @CircuitBreaker(name = "stripe", fallbackMethod = "handleSubscriptionCreationFailure")
    @Retry(name = "stripe", fallbackMethod = "handleSubscriptionCreationFailure")
    public void createSubscription(SubscriptionCreatedEvent subscriptionEvent) {
        try {
            SubscriptionCreateParams params = SubscriptionCreateParams
                    .builder()
                    .setCustomer(subscriptionEvent.customer().stripeCustomerId())
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.ALLOW_INCOMPLETE)
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(subscriptionEvent.plan().stripePriceId())
                            .build())
                    .putMetadata("subscriptionId", subscriptionEvent.subscriptionId().toString())
                    .build();

            Subscription.create(params);

        } catch (StripeException e) {
            log.error("Failed to create Stripe subscription for subscription ID: {}", subscriptionEvent.subscriptionId(), e);
            throw new StripeIntegrationException("Failed to create Stripe subscription for subscription ID: " + subscriptionEvent.subscriptionId() + ". Stripe returned an error.", e);
        }
    }

    private void handleSubscriptionCreationFailure(SubscriptionCreatedEvent event, Throwable ex) {
        log.error(
                "Failed to create Stripe subscription for subscriptionId {}. Sending event to DLQ.",
                event.subscriptionId(),
                ex
        );

        paymentEventPublisher.sendPaymentToDlqQueue(event);
        throw new StripeIntegrationException("Stripe subscription creation failed after all retry attempts for subscription ID: " + event.subscriptionId() + ". Event forwarded to DLQ.", ex);
    }
}