package com.authentication.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.model.Authentication;
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
                .withExpiresAt(tokenExpiration)
                .sign(Algorithm.HMAC256(SECRET_KEY));

        return LoginResponseDTO.builder()
                .access_token(token)
                .token_type("Bearer")
                .expires_in(tokenExpiration)
                .build();
    }

    public DecodedJWT validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

        try {
            return JWT.require(algorithm)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

}
