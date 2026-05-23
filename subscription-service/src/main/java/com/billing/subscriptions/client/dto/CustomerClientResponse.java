package com.billing.subscriptions.client.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerClientResponse(
        UUID id,
        String name,
        String lastName,
        String email,
        String phone,
        String taxNumber,
        Integer age,
        LocalDate dateOfBirth,
        CustomerAddressClientResponse address,
        String customerStatus,
        String stripeCustomerId
) {
}
