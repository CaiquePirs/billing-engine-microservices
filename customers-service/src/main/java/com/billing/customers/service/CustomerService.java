package com.billing.customers.service;

import com.billing.customers.controller.advice.exceptions.CustomerNotFoundException;
import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.mapper.CustomerMapper;
import com.billing.customers.model.Customer;
import com.billing.customers.model.enums.CustomerStatus;
import com.billing.customers.repository.CustomerRepository;
import com.billing.customers.validator.CustomerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerValidator customerValidator;
    private final CustomerMapper customerMapper;
    private final StripeCustomerService stripeCustomerService;

    public Customer createCustomer(CustomerRequestDTO customerRequestDTO) {
        customerValidator.validate(customerRequestDTO);
        Customer customer = customerMapper.toCustomer(customerRequestDTO);

        String stripeCustomerId = stripeCustomerService.createStripeCustomer(customerRequestDTO);

        customer.setStripeCustomerId(stripeCustomerId);
        return customerRepository.save(customer);
    }

    public Customer findCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .filter(customer -> customer.getCustomerStatus().equals(CustomerStatus.ACTIVE))
                .orElseThrow(() -> new CustomerNotFoundException("Customer ID not found"));
    }
}
