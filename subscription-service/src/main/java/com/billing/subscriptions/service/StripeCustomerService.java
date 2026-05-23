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
                 throw new StripeIntegrationException("Customer not found");
             }

        } catch (StripeException e) {
            log.error("Failed to fetching customer by ID {}", customerId, e);
            throw new StripeIntegrationException("Subscription failed. Try again later");
        }
    }
}
