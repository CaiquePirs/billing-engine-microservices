package com.billing.customers.service;

import com.billing.customers.controller.advice.exceptions.StripeIntegrationException;
import com.billing.customers.controller.dto.CustomerRequestDTO;
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

    public String createStripeCustomer(CustomerRequestDTO dto) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(dto.email())
                    .setName(dto.name())
                    .setPhone(dto.phone())
                    .setAddress(CustomerCreateParams.Address.builder()
                            .setCity(dto.address().city())
                            .setCountry(dto.address().county())
                            .setLine1(dto.address().street())
                            .setLine2(dto.address().number())
                            .setState(dto.address().state())
                            .setPostalCode(dto.address().eircode())
                            .build())
                    .build();

            Customer customer = Customer.create(params);
            return customer.getId();

        } catch (StripeException e) {
            log.error("Failed to create customer email: {} on Stripe and error: {}", dto.email(), e.getMessage());
            throw new StripeIntegrationException("Failed to create a customer");
        }
    }
}
