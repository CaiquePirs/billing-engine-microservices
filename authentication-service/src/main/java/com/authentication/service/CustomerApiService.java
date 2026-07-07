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
            log.warn("Customer registration conflict: an account with this email or tax number already exists");
            throw new AuthRegisterFailException("Registration failed: an account with this email or tax number already exists");

        } catch (Exception e) {
            log.error("Unexpected error during customer registration", e);
            throw new AuthRegisterFailException("Registration failed due to an unexpected error. Please try again later.");
        }
    }
}
