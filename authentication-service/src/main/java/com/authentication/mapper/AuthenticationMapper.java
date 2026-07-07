package com.authentication.mapper;

import com.authentication.controller.dto.AuthenticationResponseDTO;
import com.authentication.model.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationMapper {

    public AuthenticationResponseDTO mapToResponse(Authentication authentication){
        return AuthenticationResponseDTO.builder()
                .authenticationId(authentication.getId())
                .customerId(authentication.getCustomerId())
                .email(authentication.getEmail())
                .role(authentication.getRole())
                .build();
    }

}
