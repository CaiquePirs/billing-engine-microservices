package com.billing.customers.service;

import com.billing.customers.controller.advice.exceptions.StripeIntegrationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeCustomerService {

    public String createStripeCustomer(String email, String name) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(name)
                    .build();

            Customer customer = Customer.create(params);
            return customer.getId();

        } catch (StripeException e) {
            log.error("Failed to create customer email: {} on Stripe and error: {}", email, e.getMessage());
            throw new StripeIntegrationException("Failed to create a customer");
        }
    }
}
