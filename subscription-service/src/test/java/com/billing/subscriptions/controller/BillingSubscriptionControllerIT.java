package com.billing.subscriptions.controller;

import com.billing.subscriptions.config.SecurityConfig;
import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
import com.billing.subscriptions.controller.dto.BillingSubscriptionResponseDTO;
import com.billing.subscriptions.mapper.BillingSubscriptionMapper;
import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import com.billing.subscriptions.service.BillingSubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillingSubscriptionController.class)
@Import({SecurityConfig.class, BillingSubscriptionControllerIT.MethodSecurityConfig.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "eureka.client.enabled=false",
        "security.jwt.secret-key=test-secret-key-for-integration-test-32ch"
})
class BillingSubscriptionControllerIT {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private BillingSubscriptionService billingSubscriptionService;
    @MockitoBean private BillingSubscriptionMapper billingSubscriptionMapper;

    private static final String BASE_URL = "/api/v1/subscriptions";

    private BillingSubscriptionRequestDTO buildRequest() {
        return new BillingSubscriptionRequestDTO(UUID.randomUUID(), UUID.randomUUID(), "pm_test123");
    }

    private BillingSubscriptionResponseDTO buildResponse() {
        return BillingSubscriptionResponseDTO.builder()
                .id(UUID.randomUUID()).customerId(UUID.randomUUID())
                .currentPeriodStart(LocalDate.now()).currentPeriodEnd(LocalDate.now().plusMonths(1))
                .subscriptionStatus(SubscriptionStatus.PENDING).build();
    }

    @Test
    void createSubscription_shouldReturn201_whenAuthenticatedAsCustomer() throws Exception {
        BillingSubscription subscription = mock(BillingSubscription.class);
        when(billingSubscriptionService.createSubscription(any())).thenReturn(subscription);
        when(billingSubscriptionMapper.toResponse(subscription)).thenReturn(buildResponse());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionStatus").value("PENDING"));
    }

    @Test
    void createSubscription_shouldReturn403_whenAuthenticatedAsTenant() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TENANT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());

        verify(billingSubscriptionService, never()).createSubscription(any());
    }

    @Test
    void createSubscription_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isUnauthorized());

        verify(billingSubscriptionService, never()).createSubscription(any());
    }

    @Test
    void createSubscription_shouldReturn422_whenPaymentMethodIdIsBlank() throws Exception {
        BillingSubscriptionRequestDTO invalid = new BillingSubscriptionRequestDTO(UUID.randomUUID(), UUID.randomUUID(), "");

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity());

        verify(billingSubscriptionService, never()).createSubscription(any());
    }
}
