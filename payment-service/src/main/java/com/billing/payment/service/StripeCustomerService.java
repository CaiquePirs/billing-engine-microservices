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
                throw new StripeIntegrationException(String.format("Customer ID: %s not found", customerId));
            }
            return customer;

        } catch (StripeException e) {
            log.error("Failed to fetching customer by ID {}", customerId, e);
            throw new StripeIntegrationException("Subscription failed. Try again later");
        }
    }

}
