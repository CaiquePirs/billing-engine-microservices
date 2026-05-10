package com.authentication.controller.advice.exceptions;

public class AuthenticationRegisterFailException extends RuntimeException {
    public AuthenticationRegisterFailException(String message) {
        super(message);
    }
}
