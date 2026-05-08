package com.billing.customers.service;

import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.mapper.CustomerMapper;
import com.billing.customers.model.Customer;
import com.billing.customers.repository.CustomerRepository;
import com.billing.customers.validator.CustomerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerValidator customerValidator;
    private final CustomerMapper customerMapper;

    public Customer createCustomer(CustomerRequestDTO customerRequestDTO) {
        customerValidator.validate(customerRequestDTO);
        Customer customer = customerMapper.toCustomer(customerRequestDTO);
        return customerRepository.save(customer);
    }
}
