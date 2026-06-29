package com.authentication.controller;

import com.authentication.client.dto.CreateAddressRequestDTO;
import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.config.SecurityConfig;
import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.dto.InternalLoginRequestDTO;
import com.authentication.controller.dto.LoginRequestDTO;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.model.enums.Role;
import com.authentication.service.AuthenticationService;
import com.authentication.service.InternalAuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "eureka.client.enabled=false"
})
class AuthenticationControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthenticationService authenticationService;
    @MockBean private InternalAuthenticationService internalAuthenticationService;

    private static final String BASE_URL = "/api/v1/auth";

    private CreateCustomerRequestDTO buildSignupRequest() {
        return new CreateCustomerRequestDTO(
                "John", "Doe", "john@example.com", "P@ssword123",
                "+35312345678", "1234567AB", LocalDate.of(1990, 1, 1),
                Role.CUSTOMER,
                new CreateAddressRequestDTO("Main St", "10", "Dublin", "Leinster", "Co. Dublin", "D01AB12")
        );
    }

    private LoginResponseDTO buildLoginResponse() {
        return LoginResponseDTO.builder()
                .access_token("eyJ.mocked.token")
                .token_type("Bearer")
                .expires_in(Instant.now().plusSeconds(900))
                .build();
    }

    @Test
    void signupCustomer_shouldReturn201_whenRequestIsValid() throws Exception {
        doNothing().when(authenticationService).signupUser(any());

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSignupRequest())))
                .andExpect(status().isCreated());

        verify(authenticationService).signupUser(any());
    }

    @Test
    void signupCustomer_shouldReturn422_whenEmailIsBlank() throws Exception {
        CreateCustomerRequestDTO invalid = new CreateCustomerRequestDTO(
                "John", "Doe", "", "P@ssword123",
                "+35312345678", "1234567AB", LocalDate.of(1990, 1, 1),
                Role.CUSTOMER,
                new CreateAddressRequestDTO("Main St", "10", "Dublin", "Leinster", "Co. Dublin", "D01AB12")
        );

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity());

        verify(authenticationService, never()).signupUser(any());
    }

    @Test
    void signInCustomer_shouldReturn201_whenCredentialsAreValid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("john@example.com", "P@ssword123");
        when(authenticationService.signInUser(any())).thenReturn(buildLoginResponse());

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").value("eyJ.mocked.token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void signInCustomer_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("john@example.com", "wrong");
        when(authenticationService.signInUser(any())).thenThrow(new AuthLoginFailException("Login failed"));

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signInInternalService_shouldReturn201_whenCredentialsAreValid() throws Exception {
        InternalLoginRequestDTO request = new InternalLoginRequestDTO("svc-client", "svc-secret");
        when(internalAuthenticationService.signInInternalUser(any())).thenReturn(buildLoginResponse());

        mockMvc.perform(post(BASE_URL + "/internal-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    void signInInternalService_shouldReturn401_whenLoginFails() throws Exception {
        InternalLoginRequestDTO request = new InternalLoginRequestDTO("svc-client", "wrong-secret");
        when(internalAuthenticationService.signInInternalUser(any()))
                .thenThrow(new AuthLoginFailException("Internal login failed"));

        mockMvc.perform(post(BASE_URL + "/internal-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
