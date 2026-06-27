package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.StripeIntegrationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeCustomerService {

    public void ensureCustomerExistsOnStripe(String customerId) {
        try {
             Customer customer = Customer.retrieve(customerId);
             if(customer == null) {
                 throw new StripeIntegrationException("Stripe customer not found for ID: " + customerId);
             }

        } catch (StripeException e) {
            log.error("Stripe API error while verifying customer with ID {}", customerId, e);
            throw new StripeIntegrationException("Failed to verify Stripe customer with ID: " + customerId + ". Subscription creation aborted.");
        }
    }
}
