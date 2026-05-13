package com.authentication.controller.advice.exceptions;

public class AuthRegisterFailException extends RuntimeException {
    public AuthRegisterFailException(String message) {
        super(message);
    }
}
