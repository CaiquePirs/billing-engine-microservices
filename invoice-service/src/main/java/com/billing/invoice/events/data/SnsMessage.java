package com.billing.invoice.events.data;

public record SnsMessage(
        String Type,
        String Message
) {}