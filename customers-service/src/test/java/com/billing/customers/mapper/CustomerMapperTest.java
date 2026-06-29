package com.billing.customers.mapper;

import com.billing.customers.controller.dto.AddressRequestDTO;
import com.billing.customers.controller.dto.AddressResponseDTO;
import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.controller.dto.CustomerResponseDTO;
import com.billing.customers.model.Address;
import com.billing.customers.model.Customer;
import com.billing.customers.model.enums.CustomerStatus;
import com.billing.customers.model.enums.Role;
import com.billing.customers.validator.CustomerValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerMapperTest {

    @Mock private CustomerValidator customerValidator;

    @InjectMocks
    private CustomerMapper customerMapper;

    private AddressRequestDTO buildAddressRequest() {
        return new AddressRequestDTO("Main St", "10", "Dublin", "Leinster", "Ireland", "D01 AB12");
    }

    private CustomerRequestDTO buildCustomerRequest() {
        return new CustomerRequestDTO("John", "Doe", "john@example.com",
                "+35312345678", "123456789", LocalDate.of(1990, 1, 1), Role.CUSTOMER, buildAddressRequest());
    }

    @Test
    void toCustomer_shouldMapAllFields_whenRequestIsValid() {
        CustomerRequestDTO request = buildCustomerRequest();
        when(customerValidator.validateCustomerAge(LocalDate.of(1990, 1, 1))).thenReturn(34);

        Customer result = customerMapper.toCustomer(request);

        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getPhone()).isEqualTo("+35312345678");
        assertThat(result.getTaxNumber()).isEqualTo("123456789");
        assertThat(result.getAge()).isEqualTo(34);
        assertThat(result.getCustomerStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(result.getAddress()).isNotNull();
    }

    @Test
    void toAddress_shouldMapAllFields_whenAddressRequestIsValid() {
        AddressRequestDTO addressRequest = buildAddressRequest();

        Address result = customerMapper.toAddress(addressRequest);

        assertThat(result.getStreet()).isEqualTo("Main St");
        assertThat(result.getNumber()).isEqualTo("10");
        assertThat(result.getCity()).isEqualTo("Dublin");
        assertThat(result.getState()).isEqualTo("Leinster");
        assertThat(result.getCounty()).isEqualTo("Ireland");
        assertThat(result.getEircode()).isEqualTo("D01 AB12");
    }

    @Test
    void toResponse_shouldMapAllFields_whenCustomerIsValid() {
        Address address = Address.builder()
                .id(UUID.randomUUID())
                .street("Main St")
                .number("10")
                .city("Dublin")
                .state("Leinster")
                .county("Ireland")
                .eircode("D01 AB12")
                .build();

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .name("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("+35312345678")
                .stripeCustomerId("cus_abc123")
                .address(address)
                .build();

        CustomerResponseDTO result = customerMapper.toResponse(customer);

        assertThat(result.id()).isEqualTo(customer.getId());
        assertThat(result.name()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        assertThat(result.email()).isEqualTo("john@example.com");
        assertThat(result.phone()).isEqualTo("+35312345678");
        assertThat(result.stripeCustomerId()).isEqualTo("cus_abc123");
        assertThat(result.address()).isNotNull();
    }

    @Test
    void toAddressResponse_shouldMapAllFields_whenAddressIsValid() {
        UUID addressId = UUID.randomUUID();
        Address address = Address.builder()
                .id(addressId)
                .street("Main St")
                .number("10")
                .city("Dublin")
                .state("Leinster")
                .county("Ireland")
                .eircode("D01 AB12")
                .build();

        AddressResponseDTO result = customerMapper.toAddressResponse(address);

        assertThat(result.id()).isEqualTo(addressId);
        assertThat(result.street()).isEqualTo("Main St");
        assertThat(result.number()).isEqualTo("10");
        assertThat(result.city()).isEqualTo("Dublin");
        assertThat(result.state()).isEqualTo("Leinster");
        assertThat(result.county()).isEqualTo("Ireland");
        assertThat(result.eircode()).isEqualTo("D01 AB12");
    }
}
