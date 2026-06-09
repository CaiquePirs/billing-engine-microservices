package com.billing.subscriptions.events.data;

public record SnsMessage(
        String Type,
        String Message
) {}