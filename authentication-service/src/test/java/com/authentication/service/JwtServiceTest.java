package com.authentication.service;

import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.model.Authentication;
import com.authentication.model.InternalAuthentication;
import com.authentication.model.enums.AuthScope;
import com.authentication.model.enums.AuthStatus;
import com.authentication.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-key-that-is-long-enough-for-hmac256-algorithm");
    }

    @Test
    void generateAccessToken_shouldReturnTokenWithBearerType_whenAuthenticationIsValid() {
        Authentication authentication = Authentication.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .email("john@example.com")
                .passwordHash("hash")
                .status(AuthStatus.ACTIVE)
                .role(Role.CUSTOMER)
                .build();

        LoginResponseDTO result = jwtService.generateAccessToken(authentication);

        assertThat(result).isNotNull();
        assertThat(result.access_token()).isNotBlank();
        assertThat(result.token_type()).isEqualTo("Bearer");
        assertThat(result.expires_in()).isNotNull();
    }

    @Test
    void generateAccessToken_shouldSetExpirationInFuture_whenCalled() {
        Authentication authentication = Authentication.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .email("admin@example.com")
                .passwordHash("hash")
                .status(AuthStatus.ACTIVE)
                .role(Role.TENANT)
                .build();

        LoginResponseDTO result = jwtService.generateAccessToken(authentication);

        assertThat(result.expires_in()).isInTheFuture();
    }

    @Test
    void generateInternalToken_shouldReturnTokenWithBearerType_whenInternalAuthIsValid() {
        InternalAuthentication internalAuthentication = InternalAuthentication.builder()
                .id(UUID.randomUUID())
                .clientId("subscription-service")
                .clientSecretHash("hashed-secret")
                .scope(AuthScope.INTERNAL_SERVICE)
                .status(AuthStatus.ACTIVE)
                .build();

        LoginResponseDTO result = jwtService.generateInternalToken(internalAuthentication);

        assertThat(result).isNotNull();
        assertThat(result.access_token()).isNotBlank();
        assertThat(result.token_type()).isEqualTo("Bearer");
        assertThat(result.expires_in()).isNotNull();
    }

    @Test
    void generateInternalToken_shouldHaveLongerExpiry_thanUserAccessToken() {
        Authentication userAuth = Authentication.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hash")
                .status(AuthStatus.ACTIVE)
                .role(Role.CUSTOMER)
                .build();

        InternalAuthentication internalAuth = InternalAuthentication.builder()
                .id(UUID.randomUUID())
                .clientId("service-client")
                .clientSecretHash("hash")
                .scope(AuthScope.INTERNAL_SERVICE)
                .status(AuthStatus.ACTIVE)
                .build();

        LoginResponseDTO userToken = jwtService.generateAccessToken(userAuth);
        LoginResponseDTO internalToken = jwtService.generateInternalToken(internalAuth);

        assertThat(internalToken.expires_in()).isAfter(userToken.expires_in());
    }
}
