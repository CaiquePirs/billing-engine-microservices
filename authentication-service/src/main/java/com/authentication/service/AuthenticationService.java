package com.authentication.service;

import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.client.dto.CreateCustomerResponseDTO;
import com.authentication.client.service.CustomerApiService;
import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.dto.LoginRequestDTO;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.model.AuditEntity;
import com.authentication.model.Authentication;
import com.authentication.model.enums.AuthStatus;
import com.authentication.repository.AuthenticationRepository;
import com.authentication.validation.AuthenticationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerApiService customerApiService;
    private final AuthenticationValidator authenticationValidator;
    private final JwtService jwtService;

    public void signupUser(CreateCustomerRequestDTO createCustomerRequestDTO){
        CreateCustomerResponseDTO customerCreated = customerApiService.signupCustomer(createCustomerRequestDTO);

        Authentication authentication = Authentication.builder()
                .customerId(customerCreated.customerId())
                .email(customerCreated.email())
                .passwordHash(passwordEncoder.encode(createCustomerRequestDTO.password()))
                .status(AuthStatus.ACTIVE)
                .auditEntity(new AuditEntity())
                .build();

        authenticationRepository.save(authentication);
    }

    public LoginResponseDTO signInUser(LoginRequestDTO loginRequestDTO){
        Authentication authentication = findUserAuthenticationByEmail(loginRequestDTO.email());
        authenticationValidator.validateMatchUserPassword(loginRequestDTO.password(), authentication.getPasswordHash());
        return jwtService.generateAccessToken(authentication);
    }

    private Authentication findUserAuthenticationByEmail(String email){
        return authenticationRepository.findByEmail(email)
                .orElseThrow(() -> new AuthLoginFailException("User email not found"));
    }
}
