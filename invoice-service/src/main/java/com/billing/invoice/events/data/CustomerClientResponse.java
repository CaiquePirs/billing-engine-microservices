package com.billing.invoice.events.data;

import java.util.UUID;

public record CustomerClientResponse(
        UUID id,
        String name,
        String lastName,
        String email,
        String phone,
        CustomerAddressResponse address,
        String stripeCustomerId) {
}
