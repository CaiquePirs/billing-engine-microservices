package com.authentication.service;

import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.client.dto.CreateCustomerResponseDTO;
import com.authentication.client.service.CustomerApiService;
import com.authentication.model.Authentication;
import com.authentication.repository.AuthenticationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerApiService customerApiService;

    public void signupUser(CreateCustomerRequestDTO createCustomerRequestDTO){
        CreateCustomerResponseDTO customerCreated = customerApiService.signupCustomer(createCustomerRequestDTO);

        Authentication authentication = Authentication.builder()
                .customerId(customerCreated.customerId())
                .email(customerCreated.email())
                .passwordHash(passwordEncoder.encode(createCustomerRequestDTO.password()))
                .build();

        authenticationRepository.save(authentication);
    }
}
