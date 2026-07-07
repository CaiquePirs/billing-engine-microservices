package com.billing.invoice.advice.exceptions;

public class InvoiceGenerationException extends RuntimeException {
    public InvoiceGenerationException(String message) {
        super(message);
    }
    public InvoiceGenerationException(String message, Throwable e) {
        super(message, e);
    }
}
