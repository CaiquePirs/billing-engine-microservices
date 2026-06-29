package com.billing.customers.controller;

import com.billing.customers.config.SecurityConfig;
import com.billing.customers.controller.advice.exceptions.CustomerNotFoundException;
import com.billing.customers.controller.dto.AddressRequestDTO;
import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.controller.dto.CustomerResponseDTO;
import com.billing.customers.mapper.CustomerMapper;
import com.billing.customers.model.Customer;
import com.billing.customers.model.enums.CustomerStatus;
import com.billing.customers.model.enums.Role;
import com.billing.customers.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, CustomerControllerIT.MethodSecurityConfig.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "eureka.client.enabled=false",
        "security.jwt.secret-key=test-secret-key-for-integration-test-32ch"
})
class CustomerControllerIT {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CustomerService customerService;
    @MockitoBean private CustomerMapper customerMapper;

    private static final String BASE_URL = "/api/v1/customers";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private CustomerRequestDTO buildRequest() {
        return new CustomerRequestDTO(
                "John", "Doe", "john@example.com", "+35312345678", "1234567AB",
                LocalDate.of(1990, 1, 1), Role.CUSTOMER,
                new AddressRequestDTO("Main St", "10", "Dublin", "Leinster", "Co. Dublin", "D01AB12")
        );
    }

    private Customer buildCustomer(UUID id) {
        return Customer.builder()
                .id(id).name("John").lastName("Doe").email("john@example.com")
                .phone("+35312345678").taxNumber("1234567AB").age(34)
                .dateOfBirth(LocalDate.of(1990, 1, 1)).role(Role.CUSTOMER)
                .customerStatus(CustomerStatus.ACTIVE).stripeCustomerId("cus_test")
                .build();
    }

    @Test
    void createCustomer_shouldReturn201_whenAuthenticatedWithInternalServiceScope() throws Exception {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId);
        when(customerService.createCustomer(any())).thenReturn(customer);

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_INTERNAL_SERVICE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void createCustomer_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isUnauthorized());

        verify(customerService, never()).createCustomer(any());
    }

    @Test
    void createCustomer_shouldReturn403_whenAuthenticatedWithoutRequiredScope() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());

        verify(customerService, never()).createCustomer(any());
    }

    @Test
    void createCustomer_shouldReturn422_whenEmailIsBlank() throws Exception {
        CustomerRequestDTO invalid = new CustomerRequestDTO(
                "John", "Doe", "", "+35312345678", "1234567AB",
                LocalDate.of(1990, 1, 1), Role.CUSTOMER,
                new AddressRequestDTO("Main St", "10", "Dublin", "Leinster", "Co. Dublin", "D01AB12")
        );

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_INTERNAL_SERVICE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity());

        verify(customerService, never()).createCustomer(any());
    }

    @Test
    void findCustomerById_shouldReturn200_whenCustomerExistsAndAuthenticated() throws Exception {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId);
        CustomerResponseDTO response = CustomerResponseDTO.builder()
                .id(customerId).name("John").lastName("Doe").email("john@example.com")
                .build();

        when(customerService.findCustomerById(customerId)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/" + customerId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_INTERNAL_SERVICE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void findCustomerById_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findCustomerById_shouldReturn404_whenCustomerNotFound() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerService.findCustomerById(customerId))
                .thenThrow(new CustomerNotFoundException("Customer not found for ID: " + customerId));

        mockMvc.perform(get(BASE_URL + "/" + customerId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_INTERNAL_SERVICE"))))
                .andExpect(status().isNotFound());
    }
}
