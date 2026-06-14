package com.billing.payment.events.data;

import lombok.Builder;
import java.util.UUID;

@Builder
public record CustomerClientResponse(
        UUID id,
        String name,
        String lastName,
        String email,
        String phone,
        CustomerAddressResponse address,
        String stripeCustomerId) {
}
