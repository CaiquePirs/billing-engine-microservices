package com.billing.payment.events.data;

public record SnsMessage(
        String Type,
        String Message
) {}