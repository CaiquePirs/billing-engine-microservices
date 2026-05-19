package com.authentication.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.model.Authentication;
import com.authentication.model.InternalAuthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;


@Component
public class JwtService {

    private final String SECRET_KEY;

    public JwtService(@Value("${SECRET_KEY}") String SECRET_KEY){
        this.SECRET_KEY = SECRET_KEY;
    }

    public LoginResponseDTO generateAccessToken(Authentication authentication) {
        Instant tokenExpiration = Instant.now().plus(Duration.ofMinutes(15));

        var token = JWT.create()
                .withSubject(authentication.getId().toString())
                .withClaim("role", authentication.getRole().toString())
                .withClaim("customer_id", authentication.getCustomerId().toString())
                .withExpiresAt(tokenExpiration)
                .sign(Algorithm.HMAC256(SECRET_KEY));

        return LoginResponseDTO.builder()
                .access_token(token)
                .token_type("Bearer")
                .expires_in(tokenExpiration)
                .build();
    }

    public LoginResponseDTO generateInternalToken(InternalAuthentication authentication){
        Instant tokenExpiration = Instant.now().plus(Duration.ofMinutes(30));

        var token = JWT.create()
                .withSubject(authentication.getId().toString())
                .withClaim("scope", authentication.getScope().toString())
                .withClaim("client_id",  authentication.getClientId())
                .withExpiresAt(tokenExpiration)
                .sign(Algorithm.HMAC256(SECRET_KEY));

        return LoginResponseDTO.builder()
                .access_token(token)
                .token_type("Bearer")
                .expires_in(tokenExpiration)
                .build();
    }

}
