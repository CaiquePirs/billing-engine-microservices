package com.billing.payment.service;

import com.billing.payment.controller.advice.StripeIntegrationException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.publisher.PaymentEventPublisher;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
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
    public Subscription createSubscription(SubscriptionCreatedEvent subscriptionEvent, String stripePaymentMethodId) {
        try {
            SubscriptionCreateParams params = SubscriptionCreateParams
                    .builder()
                    .setDefaultPaymentMethod(stripePaymentMethodId)
                    .setCustomer(subscriptionEvent.customer().stripeCustomerId())
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(subscriptionEvent.plan().stripePriceId())
                            .build())
                    .putMetadata(
                            "subscriptionId",
                            subscriptionEvent.id().toString()
                    )
                    .build();

            return Subscription.create(params);

        } catch (StripeException e) {
            log.error("Failed to send payment to process subscription ID: {} on Stripe.", subscriptionEvent.id(), e);
            throw new StripeIntegrationException("Failed to process the payment.");
        }
    }

    private Subscription handleSubscriptionCreationFailure(SubscriptionCreatedEvent event, String paymentMethodId, Exception ex) {
        log.error(
                "Failed to create Stripe subscription for subscriptionId {}. Sending event to DLQ.",
                event.id(),
                ex
        );

        paymentEventPublisher.sendPaymentToDlqQueue(event);
        throw new StripeIntegrationException("Payment sent to DLQ after retries exhausted. ", ex);
    }


}