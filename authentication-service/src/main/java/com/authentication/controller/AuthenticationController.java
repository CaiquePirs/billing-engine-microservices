package com.authentication.controller;

import com.authentication.client.dto.CreateCustomerRequestDTO;
import com.authentication.controller.dto.AuthenticationResponseDTO;
import com.authentication.controller.dto.InternalLoginRequestDTO;
import com.authentication.controller.dto.LoginRequestDTO;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.mapper.AuthenticationMapper;
import com.authentication.model.Authentication;
import com.authentication.service.AuthenticationService;
import com.authentication.service.InternalAuthenticationService;
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
    private final InternalAuthenticationService internalAuthenticationService;
    private final AuthenticationMapper mapper;

    @PostMapping("/signup")
    public ResponseEntity<AuthenticationResponseDTO> signupCustomer(@RequestBody @Valid CreateCustomerRequestDTO createCustomerRequestDTO) {
        Authentication authentication = authenticationService.signupUser(createCustomerRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.mapToResponse(authentication));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> signInCustomer(@RequestBody @Valid LoginRequestDTO loginRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authenticationService.signInUser(loginRequestDTO));
    }

    @PostMapping("/internal-login")
    public ResponseEntity<LoginResponseDTO> signInInternalService(@RequestBody @Valid InternalLoginRequestDTO InternalLoginRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(internalAuthenticationService.signInInternalUser(InternalLoginRequestDTO));
    }
}
