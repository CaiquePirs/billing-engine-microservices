package com.billing.subscriptions.controller.advice.exception;

public class SubscriptionFailedException extends RuntimeException {
    public SubscriptionFailedException(String message) {
        super(message);
    }
}
