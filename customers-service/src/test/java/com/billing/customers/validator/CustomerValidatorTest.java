package com.billing.customers.validator;

import com.billing.customers.controller.advice.exceptions.CustomerExistException;
import com.billing.customers.controller.dto.AddressRequestDTO;
import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.model.Customer;
import com.billing.customers.model.enums.CustomerStatus;
import com.billing.customers.model.enums.Role;
import com.billing.customers.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerValidatorTest {

    @Mock private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerValidator customerValidator;

    private CustomerRequestDTO buildRequest(String email, String taxNumber) {
        AddressRequestDTO address = new AddressRequestDTO("Main St", "10", "Dublin", "Leinster", "Ireland", "D01 AB12");
        return new CustomerRequestDTO("John", "Doe", email, "+35312345678",
                taxNumber, LocalDate.of(1990, 1, 1), Role.CUSTOMER, address);
    }

    private Customer buildCustomer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .email("existing@example.com")
                .taxNumber("999888777")
                .customerStatus(CustomerStatus.ACTIVE)
                .build();
    }

    @Test
    void validate_shouldNotThrow_whenEmailAndTaxNumberAreUnique() {
        CustomerRequestDTO request = buildRequest("new@example.com", "111222333");
        when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(customerRepository.findByTaxNumber("111222333")).thenReturn(Optional.empty());

        assertThatCode(() -> customerValidator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldThrowCustomerExistException_whenEmailAlreadyExists() {
        CustomerRequestDTO request = buildRequest("existing@example.com", "111222333");
        when(customerRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(buildCustomer()));

        assertThatThrownBy(() -> customerValidator.validate(request))
                .isInstanceOf(CustomerExistException.class)
                .hasMessageContaining("email address is already registered");
    }

    @Test
    void validate_shouldThrowCustomerExistException_whenTaxNumberAlreadyExists() {
        CustomerRequestDTO request = buildRequest("new@example.com", "999888777");
        when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(customerRepository.findByTaxNumber("999888777")).thenReturn(Optional.of(buildCustomer()));

        assertThatThrownBy(() -> customerValidator.validate(request))
                .isInstanceOf(CustomerExistException.class)
                .hasMessageContaining("tax number is already registered");
    }

    @Test
    void validateCustomerAge_shouldReturnCorrectAge_whenDateOfBirthIsProvided() {
        int currentYear = LocalDate.now().getYear();
        LocalDate dateOfBirth = LocalDate.of(1990, 6, 15);

        int age = customerValidator.validateCustomerAge(dateOfBirth);

        assertThat(age).isEqualTo(currentYear - 1990);
    }

    @Test
    void validateCustomerAge_shouldReturnZero_whenCustomerBornThisYear() {
        int currentYear = LocalDate.now().getYear();
        LocalDate dateOfBirth = LocalDate.of(currentYear, 1, 1);

        int age = customerValidator.validateCustomerAge(dateOfBirth);

        assertThat(age).isZero();
    }
}
