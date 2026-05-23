package com.billing.subscriptions.client.dto;

import java.util.UUID;

public record CustomerAddressClientResponse(
        UUID id,
        String street,
        String number,
        String city,
        String state,
        String county,
        String eircode) {
}
