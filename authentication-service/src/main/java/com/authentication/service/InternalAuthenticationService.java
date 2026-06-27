package com.authentication.service;

import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.dto.InternalLoginRequestDTO;
import com.authentication.controller.dto.LoginResponseDTO;
import com.authentication.model.AuditEntity;
import com.authentication.model.InternalAuthentication;
import com.authentication.model.enums.AuthScope;
import com.authentication.model.enums.AuthStatus;
import com.authentication.repository.InternalAuthenticationRepository;
import com.authentication.validation.AuthenticationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalAuthenticationService {

    private final AuthenticationValidator authenticationValidator;
    private final InternalAuthenticationRepository internalAuthenticationRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public InternalAuthentication signUpInternalUser(String clientId, String clientSecret) {
        InternalAuthentication internalAuthentication = InternalAuthentication.builder()
                .clientId(clientId)
                .clientSecretHash(passwordEncoder.encode(clientSecret))
                .scope(AuthScope.INTERNAL_SERVICE)
                .status(AuthStatus.ACTIVE)
                .auditEntity(new AuditEntity())
                .build();

        return internalAuthenticationRepository.save(internalAuthentication);
    }

    public LoginResponseDTO signInInternalUser(InternalLoginRequestDTO internalLoginRequestDTO){
        InternalAuthentication authentication = findIInternalUserByClientId(internalLoginRequestDTO.clientId());

        authenticationValidator.validateMatchUserPassword(
                internalLoginRequestDTO.clientSecret(),
                authentication.getClientSecretHash()
        );

        return jwtService.generateInternalToken(authentication);
    }

    private InternalAuthentication findIInternalUserByClientId(String clientId){
        return internalAuthenticationRepository.findByClientId(clientId)
                .filter(auth -> auth.getStatus() == AuthStatus.ACTIVE)
                .orElseThrow(() -> new AuthLoginFailException("Internal service authentication failed: no active client found for ID: " + clientId));
    }

}
