package com.billing.notification.events.data;

import java.util.Map;

public record SnsMessage(
        String Type,
        String Message,
        Map<String, SnsMessageAttribute> MessageAttributes
) {
    public SnsMessage(String Type, String Message) {
        this(Type, Message, null);
    }
}
