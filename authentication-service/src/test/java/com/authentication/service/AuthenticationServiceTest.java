package com.authentication.service;

import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.client.dto.CreateCustomerResponseDTO;
import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.dto.LoginRequestDTO;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.model.Authentication;
import com.authentication.model.enums.AuthStatus;
import com.authentication.model.enums.Role;
import com.authentication.repository.AuthenticationRepository;
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
class AuthenticationServiceTest {

    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CustomerApiService customerApiService;
    @Mock private AuthenticationValidator authenticationValidator;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void signupUser_shouldSaveAuthentication_whenCustomerCreatedSuccessfully() {
        CreateCustomerRequestDTO request = mock(CreateCustomerRequestDTO.class);
        when(request.password()).thenReturn("rawPassword");
        when(request.role()).thenReturn(Role.CUSTOMER);

        CreateCustomerResponseDTO customerResponse = new CreateCustomerResponseDTO(UUID.randomUUID(), "john@example.com");
        when(customerApiService.signupCustomer(request)).thenReturn(customerResponse);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
        when(authenticationRepository.save(any(Authentication.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticationService.signupUser(request);

        verify(customerApiService).signupCustomer(request);
        verify(passwordEncoder).encode("rawPassword");
        verify(authenticationRepository).save(any(Authentication.class));
    }

    @Test
    void signupUser_shouldNotSaveAuthentication_whenCustomerApiServiceFails() {
        CreateCustomerRequestDTO request = mock(CreateCustomerRequestDTO.class);
        when(customerApiService.signupCustomer(request)).thenThrow(new RuntimeException("Customer service unavailable"));

        assertThatThrownBy(() -> authenticationService.signupUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer service unavailable");

        verify(authenticationRepository, never()).save(any());
    }

    @Test
    void signInUser_shouldReturnLoginResponse_whenCredentialsAreValid() {
        Authentication auth = Authentication.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .status(AuthStatus.ACTIVE)
                .role(Role.CUSTOMER)
                .customerId(UUID.randomUUID())
                .build();

        LoginRequestDTO loginRequest = new LoginRequestDTO("john@example.com", "rawPassword");
        LoginResponseDTO expectedResponse = LoginResponseDTO.builder()
                .access_token("jwt-token")
                .token_type("Bearer")
                .build();

        when(authenticationRepository.findByEmail("john@example.com")).thenReturn(Optional.of(auth));
        doNothing().when(authenticationValidator).validateMatchUserPassword("rawPassword", "hashedPassword");
        when(jwtService.generateAccessToken(auth)).thenReturn(expectedResponse);

        LoginResponseDTO result = authenticationService.signInUser(loginRequest);

        assertThat(result.access_token()).isEqualTo("jwt-token");
        verify(authenticationValidator).validateMatchUserPassword("rawPassword", "hashedPassword");
        verify(jwtService).generateAccessToken(auth);
    }

    @Test
    void signInUser_shouldThrowAuthLoginFailException_whenUserNotFound() {
        LoginRequestDTO loginRequest = new LoginRequestDTO("unknown@example.com", "password");
        when(authenticationRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.signInUser(loginRequest))
                .isInstanceOf(AuthLoginFailException.class)
                .hasMessageContaining("no active account found for email");

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void signInUser_shouldThrowAuthLoginFailException_whenAccountIsInactive() {
        Authentication inactiveAuth = Authentication.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .passwordHash("hash")
                .status(AuthStatus.INACTIVE)
                .role(Role.CUSTOMER)
                .customerId(UUID.randomUUID())
                .build();

        LoginRequestDTO loginRequest = new LoginRequestDTO("john@example.com", "password");
        when(authenticationRepository.findByEmail("john@example.com")).thenReturn(Optional.of(inactiveAuth));

        assertThatThrownBy(() -> authenticationService.signInUser(loginRequest))
                .isInstanceOf(AuthLoginFailException.class)
                .hasMessageContaining("no active account found for email");

        verify(jwtService, never()).generateAccessToken(any());
    }
}