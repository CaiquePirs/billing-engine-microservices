package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.UserUnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityService {

    public UUID getLoggedInAdmin(){
        SecurityContext securityContext = SecurityContextHolder.getContext();
        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) securityContext.getAuthentication();

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String currentAdminId = jwt.getClaimAsString("customer_id");

        if(currentAdminId.isBlank()){
            throw new UserUnauthorizedException("Cound not find logged in user Id");
        }
        return UUID.fromString(currentAdminId);
    }

}
