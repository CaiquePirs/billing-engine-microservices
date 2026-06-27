package com.billing.subscriptions.service;

import com.billing.subscriptions.client.api.CustomerClientApi;
import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.advice.exception.ExternalServiceException;
import com.billing.subscriptions.controller.advice.exception.NotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerApiService {

    private final CustomerClientApi api;

    public CustomerClientResponse findCustomer(UUID customerId){
        try {
            return api.findCustomerById(customerId).getBody();

        } catch (FeignException.NotFound e){
            throw new NotFoundException("Customer not found with ID: " + customerId);

        } catch (FeignException e) {
            log.error("Failed to retrieve customer from customer-service for ID {}", customerId, e);
            throw new ExternalServiceException("Failed to retrieve customer with ID: " + customerId + ". The customer-service is temporarily unavailable.");
        }
    }

}
