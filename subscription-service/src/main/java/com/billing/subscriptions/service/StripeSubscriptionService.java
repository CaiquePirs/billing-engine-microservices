package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.StripeIntegrationException;
import org.springframework.stereotype.Service;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StripeSubscriptionService {

    public Subscription createSubscription(String customerId, String priceId) {
        try {
            SubscriptionCreateParams params = SubscriptionCreateParams
                    .builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(priceId)
                            .build())
                    .build();

            return Subscription.create(params);

        } catch (StripeException e) {
            log.error("Failed to create subscription for customer ID: {} on Stripe.", customerId, e);
            throw new StripeIntegrationException("Failed to create subscription. Try again later");
        }
    }
}