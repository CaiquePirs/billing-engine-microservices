package com.billing.subscriptions.controller;

import com.billing.subscriptions.config.SecurityConfig;
import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.controller.dto.PlanResponseDTO;
import com.billing.subscriptions.mapper.PlanMapper;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.model.enums.IntervalPlan;
import com.billing.subscriptions.service.PlanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@Import({SecurityConfig.class, PlanControllerIT.MethodSecurityConfig.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "eureka.client.enabled=false",
        "security.jwt.secret-key=test-secret-key-for-integration-test-32ch"
})
class PlanControllerIT {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PlanService planService;
    @MockBean private PlanMapper planMapper;

    private static final String BASE_URL = "/api/v1/plans";

    private PlanRequestDTO buildRequest() {
        return new PlanRequestDTO("Premium", "Premium subscription plan", new BigDecimal("9900"), "EUR", IntervalPlan.MONTHLY);
    }

    private PlanResponseDTO buildResponse() {
        return PlanResponseDTO.builder()
                .id(UUID.randomUUID()).name("Premium").description("Premium subscription plan")
                .price(new BigDecimal("9900")).currency("EUR").interval("MONTHLY").active(true).build();
    }

    @Test
    void createPlan_shouldReturn201_whenAuthenticatedAsTenant() throws Exception {
        Plan plan = mock(Plan.class);
        when(planService.createPlan(any())).thenReturn(plan);
        when(planMapper.toResponse(plan)).thenReturn(buildResponse());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TENANT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Premium"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void createPlan_shouldReturn403_whenAuthenticatedAsCustomer() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());

        verify(planService, never()).createPlan(any());
    }

    @Test
    void createPlan_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isUnauthorized());

        verify(planService, never()).createPlan(any());
    }

    @Test
    void createPlan_shouldReturn422_whenNameIsTooShort() throws Exception {
        PlanRequestDTO invalid = new PlanRequestDTO("AB", "Premium subscription plan", new BigDecimal("9900"), "EUR", IntervalPlan.MONTHLY);

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TENANT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity());

        verify(planService, never()).createPlan(any());
    }
}
