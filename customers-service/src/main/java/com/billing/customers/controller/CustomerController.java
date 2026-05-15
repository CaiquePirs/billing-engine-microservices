package com.billing.customers.controller;

import com.billing.customers.controller.dto.CustomerCreatedResponseDTO;
import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.model.Customer;
import com.billing.customers.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_INTERNAL_SERVICE')")
    public ResponseEntity<CustomerCreatedResponseDTO> createCustomer(@RequestBody @Valid CustomerRequestDTO dto) {
        Customer customer = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CustomerCreatedResponseDTO(customer.getId(), customer.getEmail()));
    }

}
