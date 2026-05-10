package com.authentication.controller;

import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<Void> createCustomer(@RequestBody @Valid CreateCustomerRequestDTO createCustomerRequestDTO) {
        authenticationService.signupUser(createCustomerRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
