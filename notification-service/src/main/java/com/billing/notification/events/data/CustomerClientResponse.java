package com.billing.payment.events.data;

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
        CustomerAddressResponse address,
        String customerStatus,
        String stripeCustomerId
) {
}
