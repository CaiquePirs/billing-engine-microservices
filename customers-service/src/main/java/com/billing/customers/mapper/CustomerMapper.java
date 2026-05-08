package com.billing.customers.mapper;

import com.billing.customers.controller.dto.AddressRequestDTO;
import com.billing.customers.controller.dto.AddressResponseDTO;
import com.billing.customers.controller.dto.CustomerRequestDTO;
import com.billing.customers.controller.dto.CustomerResponseDTO;
import com.billing.customers.model.Address;
import com.billing.customers.model.Customer;
import com.billing.customers.model.CustomerStatus;
import com.billing.customers.validator.CustomerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerMapper {

    private final CustomerValidator customerValidator;

    public Customer toCustomer(CustomerRequestDTO customerRequestDTO) {
        return Customer.builder()
                .name(customerRequestDTO.name())
                .lastName(customerRequestDTO.lastName())
                .age(customerValidator.validateCustomerAge(customerRequestDTO.dateOfBirth()))
                .phone(customerRequestDTO.phone())
                .email(customerRequestDTO.email())
                .taxNumber(customerRequestDTO.taxNumber())
                .dateOfBirth(customerRequestDTO.dateOfBirth())
                .address(toAddress(customerRequestDTO.address()))
                .customerStatus(CustomerStatus.ACTIVE)
                .build();
    }

    public Address toAddress(AddressRequestDTO addressRequestDTO) {
        return Address.builder()
                .state(addressRequestDTO.state())
                .city(addressRequestDTO.city())
                .street(addressRequestDTO.street())
                .number(addressRequestDTO.number())
                .county(addressRequestDTO.county())
                .eircode(addressRequestDTO.eircode())
                .build();
    }

    public CustomerResponseDTO toResponse(Customer customer) {
        return CustomerResponseDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .lastName(customer.getLastName())
                .age(customer.getAge())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .taxNumber(customer.getTaxNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .address(toAddressResponse(customer.getAddress()))
                .customerStatus(customer.getCustomerStatus())
                .build();
    }

    public AddressResponseDTO toAddressResponse(Address address) {
        return AddressResponseDTO.builder()
                .id(address.getId())
                .state(address.getState())
                .street(address.getStreet())
                .number(address.getNumber())
                .county(address.getCounty())
                .eircode(address.getEircode())
                .city(address.getCity())
                .build();
    }
}
