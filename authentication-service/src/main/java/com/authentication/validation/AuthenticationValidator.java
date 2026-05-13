package com.authentication.validation;

import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.advice.exceptions.AuthRegisterFailException;
import com.authentication.repository.AuthenticationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationValidator {

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;

    public void validateIfExistsUserEmail(String email){
        boolean existByEmail = authenticationRepository.findByEmail(email).isPresent();
        if(existByEmail){
            throw new AuthRegisterFailException("User email already exists");
        }
    }

    public void validateMatchUserPassword(String rawPassword, String hashPassword){
        boolean matchPassword = passwordEncoder.matches(rawPassword, hashPassword);
        if(!matchPassword){
            throw new AuthLoginFailException("Password does not match");
        }
    }
}
