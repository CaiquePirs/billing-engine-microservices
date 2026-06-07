package com.billing.notification.events.data;

public record SnsMessage(
        String Type,
        String Message
) {}