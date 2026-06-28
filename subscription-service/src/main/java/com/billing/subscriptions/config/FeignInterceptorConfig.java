package com.billing.subscriptions.config;

import com.billing.subscriptions.client.dto.LoginClientRequest;
import com.billing.subscriptions.client.dto.LoginClientResponse;
import com.billing.subscriptions.controller.advice.exception.ExternalServiceException;
import com.billing.subscriptions.service.AuthenticationApiService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignInterceptorConfig implements RequestInterceptor {

    private final AuthenticationApiService api;

    @Value("${subscription.service.client-id}")
    private String clientId;

    @Value("${subscription.service.client-secret}")
    private String clientSecret;

    @Override
    public void apply(RequestTemplate template) {
        try {
            LoginClientResponse login = api.getAccessToken(new LoginClientRequest(clientId, clientSecret));
            template.header("Authorization", "Bearer " + login.access_token());

        } catch (Exception e){
            log.error("Failed to obtain access token for internal service client ID '{}'. Feign requests will not be authorized.", clientId, e);
            throw new ExternalServiceException("Internal service authentication failed for client ID: " + clientId + ". Could not obtain access token to authorize outbound requests.");
        }
    }
}
