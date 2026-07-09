package com.billing.subscriptions.events.data;

/**
 * Single SNS message attribute as it arrives inside the SNS-to-SQS envelope
 * (shape: {"Type":"String","Value":"..."}). Used to carry the W3C traceparent
 * so the async trace can be continued by the consumer.
 */
public record SnsMessageAttribute(
        String Type,
        String Value
) {}
