package com.authentication.service;

import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.dto.InternalLoginRequestDTO;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.model.AuditEntity;
import com.authentication.model.InternalAuthentication;
import com.authentication.model.enums.AuthScope;
import com.authentication.model.enums.AuthStatus;
import com.authentication.repository.InternalAuthenticationRepository;
import com.authentication.validation.AuthenticationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalAuthenticationServiceTest {

    @Mock private AuthenticationValidator authenticationValidator;
    @Mock private InternalAuthenticationRepository internalAuthenticationRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InternalAuthenticationService internalAuthenticationService;

    @Test
    void signUpInternalUser_shouldSaveAndReturnInternalAuthentication_whenCalled() {
        InternalAuthentication saved = InternalAuthentication.builder()
                .id(UUID.randomUUID())
                .clientId("subscription-service")
                .clientSecretHash("hashed-secret")
                .scope(AuthScope.INTERNAL_SERVICE)
                .status(AuthStatus.ACTIVE)
                .auditEntity(new AuditEntity())
                .build();

        when(passwordEncoder.encode("raw-secret")).thenReturn("hashed-secret");
        when(internalAuthenticationRepository.save(any(InternalAuthentication.class))).thenReturn(saved);

        InternalAuthentication result = internalAuthenticationService.signUpInternalUser("subscription-service", "raw-secret");

        assertThat(result.getClientId()).isEqualTo("subscription-service");
        assertThat(result.getScope()).isEqualTo(AuthScope.INTERNAL_SERVICE);
        assertThat(result.getStatus()).isEqualTo(AuthStatus.ACTIVE);
        verify(internalAuthenticationRepository).save(any(InternalAuthentication.class));
    }

    @Test
    void signInInternalUser_shouldReturnLoginResponse_whenCredentialsAreValid() {
        InternalAuthentication auth = InternalAuthentication.builder()
                .id(UUID.randomUUID())
                .clientId("subscription-service")
                .clientSecretHash("hashed-secret")
                .scope(AuthScope.INTERNAL_SERVICE)
                .status(AuthStatus.ACTIVE)
                .build();

        LoginResponseDTO expectedResponse = LoginResponseDTO.builder()
                .access_token("internal-jwt-token")
                .token_type("Bearer")
                .build();

        InternalLoginRequestDTO loginRequest = new InternalLoginRequestDTO("subscription-service", "raw-secret");

        when(internalAuthenticationRepository.findByClientId("subscription-service")).thenReturn(Optional.of(auth));
        doNothing().when(authenticationValidator).validateMatchUserPassword("raw-secret", "hashed-secret");
        when(jwtService.generateInternalToken(auth)).thenReturn(expectedResponse);

        LoginResponseDTO result = internalAuthenticationService.signInInternalUser(loginRequest);

        assertThat(result.access_token()).isEqualTo("internal-jwt-token");
        verify(jwtService).generateInternalToken(auth);
    }

    @Test
    void signInInternalUser_shouldThrowAuthLoginFailException_whenClientNotFound() {
        InternalLoginRequestDTO loginRequest = new InternalLoginRequestDTO("unknown-service", "secret");
        when(internalAuthenticationRepository.findByClientId("unknown-service")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalAuthenticationService.signInInternalUser(loginRequest))
                .isInstanceOf(AuthLoginFailException.class)
                .hasMessageContaining("no active client found for ID");

        verify(jwtService, never()).generateInternalToken(any());
    }

    @Test
    void signInInternalUser_shouldThrowAuthLoginFailException_whenClientIsInactive() {
        InternalAuthentication inactiveAuth = InternalAuthentication.builder()
                .id(UUID.randomUUID())
                .clientId("subscription-service")
                .clientSecretHash("hash")
                .scope(AuthScope.INTERNAL_SERVICE)
                .status(AuthStatus.INACTIVE)
                .build();

        InternalLoginRequestDTO loginRequest = new InternalLoginRequestDTO("subscription-service", "secret");
        when(internalAuthenticationRepository.findByClientId("subscription-service")).thenReturn(Optional.of(inactiveAuth));

        assertThatThrownBy(() -> internalAuthenticationService.signInInternalUser(loginRequest))
                .isInstanceOf(AuthLoginFailException.class)
                .hasMessageContaining("no active client found for ID");

        verify(jwtService, never()).generateInternalToken(any());
    }
}
