package com.billing.subscriptions.client.dto;

import java.time.Instant;

public record LoginClientResponse(
        String access_token,
        String token_type,
        Instant expires_in) {
}
