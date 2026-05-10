package com.authentication.validation;

import com.authentication.controller.advice.exceptions.AuthenticationRegisterFailException;
import com.authentication.repository.AuthenticationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationValidator {

    private final AuthenticationRepository authenticationRepository;

    public void validateIfExistsUserEmail(String email){
        boolean existByEmail = authenticationRepository.findByEmail(email).isPresent();
        if(existByEmail){
            throw new AuthenticationRegisterFailException("User email already exists");
        }
    }
}
