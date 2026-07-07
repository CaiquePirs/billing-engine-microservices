package com.authentication.config;

import com.authentication.model.InternalAuthentication;
import com.authentication.repository.InternalAuthenticationRepository;
import com.authentication.service.InternalAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InternalServiceSeederConfig implements ApplicationRunner {

    private final InternalAuthenticationService internalAuthenticationService;
    private final InternalAuthenticationRepository internalAuthenticationRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedService(System.getenv("AUTHENTICATION_SERVICE_CLIENT_ID"), System.getenv("AUTHENTICATION_SERVICE_SECRET"));
        seedService(System.getenv("SUBSCRIPTION_SERVICE_CLIENT_ID"), System.getenv("SUBSCRIPTION_SERVICE_SECRET"));
    }

    private void seedService(String clientId, String clientSecret) {
        if (internalAuthenticationRepository.existsByClientId(clientId)) {
            return;
        }
        InternalAuthentication authentication = internalAuthenticationService.signUpInternalUser(
                clientId, clientSecret
        );

        internalAuthenticationRepository.save(authentication);
        log.info("Internal service credentials seeded successfully (authId={}, scope={})",
                authentication.getId(), authentication.getScope());
    }

}
