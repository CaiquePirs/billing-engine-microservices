package com.billing.subscriptions.service;

import com.billing.subscriptions.client.api.AuthenticationClientApi;
import com.billing.subscriptions.client.dto.LoginClientRequest;
import com.billing.subscriptions.client.dto.LoginClientResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationApiService {

    private final AuthenticationClientApi authenticationClientApi;

    public LoginClientResponse getAccessToken(LoginClientRequest loginClientRequest) throws Exception {
        return authenticationClientApi
                .signInInternalService(loginClientRequest)
                .getBody();
    }
}
