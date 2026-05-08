package com.billing.customers.controller.advice.exceptions;

public class CustomerExistException extends RuntimeException {
    public CustomerExistException(String message) {
        super(message);
    }
}
