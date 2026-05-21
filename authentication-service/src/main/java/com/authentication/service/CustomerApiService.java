package com.authentication.service;

import com.authentication.client.api.CustomerClientApi;
import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.client.dto.CreateCustomerResponseDTO;
import com.authentication.controller.advice.exceptions.AuthRegisterFailException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerApiService {

    private final CustomerClientApi customerClientApi;

    public CreateCustomerResponseDTO signupCustomer(CreateCustomerRequestDTO createCustomerRequestDTO) {
        try {
            return customerClientApi.signupUser(createCustomerRequestDTO).getBody();

        } catch (FeignException.Conflict e){
            throw new AuthRegisterFailException("Customer signup failed, user already exists");

        } catch (Exception e) {
            log.error("Error when signup the customer {} ", e.getMessage());
            throw new AuthRegisterFailException("Customer signup failed.");
        }
    }
}
