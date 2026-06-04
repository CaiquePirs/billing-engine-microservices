package com.billing.payment.utils;

import com.billing.payment.controller.advice.InternalErrorException;
import com.billing.payment.controller.advice.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentUtils {

    private final ObjectMapper objectMapper;

    public UUID getInternalSubscriptionId(Event event) {
        try {
            JsonNode root = objectMapper.readTree(event.getDataObjectDeserializer().getRawJson());

            String subscriptionId =
                    root.path("parent")
                            .path("subscription_details")
                            .path("metadata")
                            .path("subscriptionId")
                            .asText();

            if (subscriptionId.isBlank()) {
                log.error("SubscriptionId metadata not found in Stripe event {}", event.getId());
                throw new NotFoundException("SubscriptionId metadata not found in Stripe event " + event.getId());
            }
            return UUID.fromString(subscriptionId);

        } catch (Exception e) {
            log.error("Failed to extract subscriptionId from Stripe event: {} ", event.getId(), e);
            throw new InternalErrorException( "Failed to extract subscriptionId from Stripe event: " + event.getId());
        }
    }
}
