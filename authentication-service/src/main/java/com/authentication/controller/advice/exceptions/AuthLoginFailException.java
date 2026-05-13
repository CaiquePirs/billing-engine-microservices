package com.authentication.controller.advice.exceptions;

public class AuthLoginFailException extends RuntimeException {
    public AuthLoginFailException(String message) {
        super(message);
    }
}
