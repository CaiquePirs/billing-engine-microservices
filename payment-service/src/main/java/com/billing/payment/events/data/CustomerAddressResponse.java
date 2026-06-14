package com.billing.payment.events.data;

import lombok.Builder;
import java.util.UUID;

@Builder
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
