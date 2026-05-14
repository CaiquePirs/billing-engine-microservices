package com.authentication.config;

import com.authentication.model.InternalAuthentication;
import com.authentication.repository.AuthenticationRepository;
import com.authentication.repository.InternalAuthenticationRepository;
import com.authentication.service.InternalAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class InternalServiceSeederConfig implements ApplicationRunner {

    private final InternalAuthenticationService internalAuthenticationService;
    private final InternalAuthenticationRepository internalAuthenticationRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedService("authentication-service", System.getenv("AUTHENTICATION_SERVICE_SECRET"));
    }

    private void seedService(String clientId, String clientSecret) {
        if (internalAuthenticationRepository.existsByClientId(clientId)) {
            return;
        }
        InternalAuthentication authentication = internalAuthenticationService.signUpInternalUser(
                clientId, clientSecret
        );
        internalAuthenticationRepository.save(authentication);
    }

}
