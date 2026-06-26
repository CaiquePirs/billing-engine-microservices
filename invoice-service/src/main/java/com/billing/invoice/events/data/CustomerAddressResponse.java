package com.billing.invoice.events.data;

import java.util.UUID;

public record CustomerAddressResponse(
        UUID id,
        String street,
        String number,
        String city,
        String state,
        String county,
        String eircode
) {
}
