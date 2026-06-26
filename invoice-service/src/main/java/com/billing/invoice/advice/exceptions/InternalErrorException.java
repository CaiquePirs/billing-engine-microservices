package com.billing.invoice.advice.exceptions;

public class InternalErrorException extends RuntimeException {
    public InternalErrorException(String message) {
        super(message);
    }
    public InternalErrorException(String message, Throwable e) {
        super(message, e);
    }
}
