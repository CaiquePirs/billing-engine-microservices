package com.authentication.validation;

import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.advice.exceptions.AuthRegisterFailException;
import com.authentication.model.Authentication;
import com.authentication.model.enums.AuthStatus;
import com.authentication.model.enums.Role;
import com.authentication.repository.AuthenticationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationValidatorTest {

    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationValidator authenticationValidator;

    @Test
    void validateIfExistsUserEmail_shouldNotThrow_whenEmailIsNotRegistered() {
        when(authenticationRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        assertThatCode(() -> authenticationValidator.validateIfExistsUserEmail("new@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateIfExistsUserEmail_shouldThrowAuthRegisterFailException_whenEmailAlreadyExists() {
        Authentication existing = Authentication.builder()
                .id(UUID.randomUUID())
                .email("existing@example.com")
                .status(AuthStatus.ACTIVE)
                .role(Role.CUSTOMER)
                .customerId(UUID.randomUUID())
                .build();

        when(authenticationRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authenticationValidator.validateIfExistsUserEmail("existing@example.com"))
                .isInstanceOf(AuthRegisterFailException.class)
                .hasMessageContaining("email address is already in use");
    }

    @Test
    void validateMatchUserPassword_shouldNotThrow_whenPasswordMatchesHash() {
        when(passwordEncoder.matches("rawPassword", "hashedPassword")).thenReturn(true);

        assertThatCode(() -> authenticationValidator.validateMatchUserPassword("rawPassword", "hashedPassword"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateMatchUserPassword_shouldThrowAuthLoginFailException_whenPasswordDoesNotMatch() {
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authenticationValidator.validateMatchUserPassword("wrongPassword", "hashedPassword"))
                .isInstanceOf(AuthLoginFailException.class)
                .hasMessageContaining("password is incorrect");
    }
}