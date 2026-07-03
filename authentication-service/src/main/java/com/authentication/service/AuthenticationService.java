package com.authentication.service;

import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.client.dto.CreateCustomerResponseDTO;
import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.dto.LoginRequestDTO;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.metrics.AuthenticationMetrics;
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
    private final AuthenticationMetrics authenticationMetrics;

    public void signupUser(CreateCustomerRequestDTO createCustomerRequestDTO){
        CreateCustomerResponseDTO customerCreated = customerApiService.signupCustomer(createCustomerRequestDTO);

        Authentication authentication = Authentication.builder()
                .customerId(customerCreated.customerId())
                .email(customerCreated.email())
                .passwordHash(passwordEncoder.encode(createCustomerRequestDTO.password()))
                .status(AuthStatus.ACTIVE)
                .role(createCustomerRequestDTO.role())
                .auditEntity(new AuditEntity())
                .build();

        authenticationRepository.save(authentication);
        authenticationMetrics.recordSignup();
    }

    public LoginResponseDTO signInUser(LoginRequestDTO loginRequestDTO){
        try {
            Authentication authentication = findUserAuthenticationByEmail(loginRequestDTO.email());
            authenticationValidator.validateMatchUserPassword(loginRequestDTO.password(), authentication.getPasswordHash());
            LoginResponseDTO response = jwtService.generateAccessToken(authentication);
            authenticationMetrics.recordLogin(true);
            return response;
        } catch (AuthLoginFailException ex) {
            authenticationMetrics.recordLogin(false);
            throw ex;
        }
    }

    private Authentication findUserAuthenticationByEmail(String email){
        return authenticationRepository.findByEmail(email)
                .filter(auth -> auth.getStatus() == AuthStatus.ACTIVE)
                .orElseThrow(() -> new AuthLoginFailException("Authentication failed: no active account found for email: " + email));
    }
}
