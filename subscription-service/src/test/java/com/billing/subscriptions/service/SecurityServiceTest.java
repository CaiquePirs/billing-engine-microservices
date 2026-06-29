package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.UserUnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @InjectMocks
    private SecurityService securityService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getLoggedInAdmin_shouldReturnAdminId_whenJwtContainsCustomerId() {
        UUID adminId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("customer_id")).thenReturn(adminId.toString());

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID result = securityService.getLoggedInAdmin();

        assertThat(result).isEqualTo(adminId);
    }

    @Test
    void getLoggedInAdmin_shouldThrowUserUnauthorizedException_whenCustomerIdIsBlank() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("customer_id")).thenReturn("");

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> securityService.getLoggedInAdmin())
                .isInstanceOf(UserUnauthorizedException.class)
                .hasMessageContaining("could not extract user ID from the JWT token");
    }
}
