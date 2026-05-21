package com.billing.customers.controller.advice.exceptions;

public class StripeIntegrationException extends RuntimeException {
    public StripeIntegrationException(String message) {
        super(message);
    }
}
