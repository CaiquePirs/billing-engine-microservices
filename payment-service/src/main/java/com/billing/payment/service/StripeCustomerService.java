package com.billing.payment.service;

import com.billing.payment.controller.advice.exceptions.StripeIntegrationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeCustomerService {

    public Customer findCustomerOnStripeById(String customerId) {
        try {
            Customer customer = Customer.retrieve(customerId);

            if(customer == null) {
                throw new StripeIntegrationException(String.format("Stripe customer not found for ID: %s", customerId));
            }
            return customer;

        } catch (StripeException e) {
            log.error("Stripe API error while retrieving customer with ID {}", customerId, e);
            throw new StripeIntegrationException("Failed to retrieve Stripe customer with ID: " + customerId + ". Subscription processing aborted.");
        }
    }

}
