package com.authentication.config;

import com.authentication.controller.dto.InternalLoginRequestDTO;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.service.InternalAuthenticationService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RequestInterceptorConfig implements RequestInterceptor {

    private final InternalAuthenticationService service;

    @Value("${auth.service.client-id}")
    private String clientId;

    @Value("${auth.service.client-secret}")
    private String clientSecret;

    @Override
    public void apply(RequestTemplate template) {
        try {
            LoginResponseDTO login = service.signInInternalUser(new InternalLoginRequestDTO(clientId, clientSecret));
            template.header("Authorization", "Bearer " + login.access_token());

        } catch (Exception e){
            log.error("Exception occurred while processing request", e);
        }
    }
}
