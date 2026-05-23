package com.billing.subscriptions.client.dto;

public record LoginClientRequest(
        String clientId,
        String clientSecret) {
}
