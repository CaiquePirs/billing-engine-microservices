package com.billing.customers.validator;

import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.controller.advice.exceptions.CustomerExistException;
import com.billing.customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CustomerValidator {

    private final CustomerRepository customerRepository;

    public void validate(CustomerRequestDTO customerRequestDTO) {
        validateEmail(customerRequestDTO.email());
        validateTaxNumber(customerRequestDTO.taxNumber());
    }

    public Integer validateCustomerAge(LocalDate customerDateBirth){
       return LocalDate.now().getYear() - customerDateBirth.getYear();
    }

    private void validateEmail(String email) {
        customerRepository.findByEmail(email)
                .ifPresent(customer -> {
                    throw new CustomerExistException("Registration failed: a customer with this email address is already registered");
                });
    }

    private void validateTaxNumber(String taxNumber) {
        customerRepository.findByTaxNumber(taxNumber)
                .ifPresent(customer -> {
                    throw new CustomerExistException("Registration failed: a customer with this tax number is already registered");
                });
    }

}
