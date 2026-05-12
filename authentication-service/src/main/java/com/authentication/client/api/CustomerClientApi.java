package com.authentication.client.api;

import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.client.dto.CreateCustomerResponseDTO;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "customers-service")
public interface CustomerClientApi {

    @PostMapping("/api/v1/customers")
    ResponseEntity<CreateCustomerResponseDTO> signupUser(@RequestBody @Valid CreateCustomerRequestDTO createCustomerRequestDTO);
}
