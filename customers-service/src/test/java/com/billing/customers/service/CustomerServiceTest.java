package com.billing.customers.service;

import com.billing.customers.controller.advice.exceptions.CustomerNotFoundException;
import com.billing.customers.controller.dto.AddressRequestDTO;
import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.mapper.CustomerMapper;
import com.billing.customers.model.Customer;
import com.billing.customers.model.enums.CustomerStatus;
import com.billing.customers.model.enums.Role;
import com.billing.customers.repository.CustomerRepository;
import com.billing.customers.validator.CustomerValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerValidator customerValidator;
    @Mock private CustomerMapper customerMapper;
    @Mock private StripeCustomerService stripeCustomerService;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequestDTO buildRequest() {
        AddressRequestDTO address = new AddressRequestDTO("Main St", "10", "Dublin", "Leinster", "Ireland", "D01 AB12");
        return new CustomerRequestDTO("John", "Doe", "john@example.com", "+35312345678",
                "123456789", LocalDate.of(1990, 1, 1), Role.CUSTOMER, address);
    }

    private Customer buildCustomer(CustomerStatus status) {
        return Customer.builder()
                .id(UUID.randomUUID())
                .name("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("+35312345678")
                .taxNumber("123456789")
                .age(34)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .customerStatus(status)
                .role(Role.CUSTOMER)
                .stripeCustomerId("cus_abc123")
                .build();
    }

    @Test
    void createCustomer_shouldReturnSavedCustomer_whenDataIsValid() {
        CustomerRequestDTO request = buildRequest();
        Customer mappedCustomer = buildCustomer(CustomerStatus.ACTIVE);
        mappedCustomer.setStripeCustomerId(null);

        doNothing().when(customerValidator).validate(request);
        when(customerMapper.toCustomer(request)).thenReturn(mappedCustomer);
        when(stripeCustomerService.createStripeCustomer(request)).thenReturn("cus_abc123");
        when(customerRepository.save(mappedCustomer)).thenReturn(mappedCustomer);

        Customer result = customerService.createCustomer(request);

        assertThat(result).isNotNull();
        assertThat(result.getStripeCustomerId()).isEqualTo("cus_abc123");
        verify(customerValidator).validate(request);
        verify(stripeCustomerService).createStripeCustomer(request);
        verify(customerRepository).save(mappedCustomer);
    }

    @Test
    void createCustomer_shouldNotSaveCustomer_whenValidationFails() {
        CustomerRequestDTO request = buildRequest();
        doThrow(new RuntimeException("Validation failed")).when(customerValidator).validate(request);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Validation failed");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void createCustomer_shouldNotSaveCustomer_whenStripeServiceFails() {
        CustomerRequestDTO request = buildRequest();
        Customer mappedCustomer = buildCustomer(CustomerStatus.ACTIVE);

        doNothing().when(customerValidator).validate(request);
        when(customerMapper.toCustomer(request)).thenReturn(mappedCustomer);
        when(stripeCustomerService.createStripeCustomer(request)).thenThrow(new RuntimeException("Stripe error"));

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stripe error");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void findCustomerById_shouldReturnCustomer_whenCustomerIsActive() {
        UUID customerId = UUID.randomUUID();
        Customer activeCustomer = buildCustomer(CustomerStatus.ACTIVE);
        activeCustomer.setId(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(activeCustomer));

        Customer result = customerService.findCustomerById(customerId);

        assertThat(result.getId()).isEqualTo(customerId);
        assertThat(result.getCustomerStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void findCustomerById_shouldThrowCustomerNotFoundException_whenCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findCustomerById(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found or inactive");
    }

    @Test
    void findCustomerById_shouldThrowCustomerNotFoundException_whenCustomerIsInactive() {
        UUID customerId = UUID.randomUUID();
        Customer inactiveCustomer = buildCustomer(CustomerStatus.INACTIVE);
        inactiveCustomer.setId(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(inactiveCustomer));

        assertThatThrownBy(() -> customerService.findCustomerById(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found or inactive");
    }
}
