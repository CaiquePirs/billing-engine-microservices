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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerApiService customerApiService;
    private final AuthenticationValidator authenticationValidator;
    private final JwtService jwtService;
    private final AuthenticationMetrics authenticationMetrics;

    public Authentication signupUser(CreateCustomerRequestDTO createCustomerRequestDTO){
        CreateCustomerResponseDTO customerCreated = customerApiService.signupCustomer(createCustomerRequestDTO);

        Authentication authentication = Authentication.builder()
                .customerId(customerCreated.customerId())
                .email(customerCreated.email())
                .passwordHash(passwordEncoder.encode(createCustomerRequestDTO.password()))
                .status(AuthStatus.ACTIVE)
                .role(createCustomerRequestDTO.role())
                .auditEntity(new AuditEntity())
                .build();

        Authentication authCreated = authenticationRepository.save(authentication);

        authenticationMetrics.recordSignup();
        log.info("Customer signed up successfully (customerId={})", customerCreated.customerId());

        return authCreated;
    }

    public LoginResponseDTO signInUser(LoginRequestDTO loginRequestDTO){
        try {
            Authentication authentication = findUserAuthenticationByEmail(loginRequestDTO.email());
            authenticationValidator.validateMatchUserPassword(loginRequestDTO.password(), authentication.getPasswordHash());

            LoginResponseDTO response = jwtService.generateAccessToken(authentication);

            authenticationMetrics.recordLogin(true);
            log.info("Login successfully (customerId={})", authentication.getCustomerId());

            return response;

        } catch (AuthLoginFailException ex) {
            authenticationMetrics.recordLogin(false);
            log.warn("Sign-in failed for email={}: invalid email or password", loginRequestDTO.email());
            throw new AuthLoginFailException("Sign-in failed: invalid email or password");
        }
    }

    private Authentication findUserAuthenticationByEmail(String email){
        return authenticationRepository.findByEmail(email)
                .filter(auth -> auth.getStatus() == AuthStatus.ACTIVE)
                .orElseThrow(() -> new AuthLoginFailException("Authentication failed: no active account found for email: " + email));
    }
}
