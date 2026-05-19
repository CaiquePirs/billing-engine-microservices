package com.billing.subscriptions.controller.advice.exception;

public class StripeIntegrationException extends RuntimeException {
    public StripeIntegrationException(String message) {
        super(message);
    }
}
