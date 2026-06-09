package com.billing.payment.utils;

import com.billing.payment.controller.advice.InternalErrorException;
import com.billing.payment.controller.advice.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    public String getStripeSubscriptionId(Event event) {
        try {
            JsonNode root = objectMapper.readTree(event.getDataObjectDeserializer().getRawJson());

            String stripeSubscriptionId = root.path("parent")
                    .path("subscription_details")
                    .path("subscription")
                    .asText();

            if (stripeSubscriptionId.isBlank()) {
                stripeSubscriptionId = getFirstInvoiceLine(event)
                        .path("parent")
                        .path("subscription_item_details")
                        .path("subscription")
                        .asText();
            }

            if (stripeSubscriptionId.isBlank()) {
                log.error("Stripe subscription id not found in Stripe invoice.paid event {}", event.getId());
                throw new NotFoundException("Stripe subscription id not found in Stripe invoice.paid event " + event.getId());
            }

            return stripeSubscriptionId;

        } catch (Exception e) {
            log.error("Failed to extract Stripe subscription id from Stripe event {}", event.getId(), e);
            throw new InternalErrorException("Failed to extract Stripe subscription id from Stripe event " + event.getId());
        }
    }

    public LocalDate getCurrentPeriodStart(Event event) {
        try {
            long periodStart = getFirstInvoiceLine(event)
                    .path("period")
                    .path("start")
                    .asLong();

            if (periodStart == 0) {
                log.error("Current period start not found in Stripe invoice.paid event {}", event.getId());
                throw new NotFoundException("Current period start not found in Stripe invoice.paid event " + event.getId());
            }

            return toLocalDate(periodStart);

        } catch (Exception e) {
            log.error("Failed to extract current period start from Stripe event {}", event.getId(), e);
            throw new InternalErrorException("Failed to extract current period start from Stripe event " + event.getId());
        }
    }

    public LocalDate getCurrentPeriodEnd(Event event) {
        try {
            long periodEnd = getFirstInvoiceLine(event)
                    .path("period")
                    .path("end")
                    .asLong();

            if (periodEnd == 0) {
                log.error("Current period end not found in Stripe invoice.paid event {}", event.getId());
                throw new NotFoundException("Current period end not found in Stripe invoice.paid event " + event.getId());
            }

            return toLocalDate(periodEnd);

        } catch (Exception e) {
            log.error("Failed to extract current period end from Stripe event {}", event.getId(), e);
            throw new InternalErrorException("Failed to extract current period end from Stripe event " + event.getId());
        }
    }

    private JsonNode getFirstInvoiceLine(Event event) {
        try {
            JsonNode root = objectMapper.readTree(event.getDataObjectDeserializer().getRawJson());

            JsonNode firstLine = root.path("lines")
                    .path("data")
                    .path(0);

            if (firstLine.isMissingNode()) {
                log.error("Invoice line item not found in Stripe invoice.paid event {}", event.getId());
                throw new NotFoundException("Invoice line item not found in Stripe invoice.paid event " + event.getId());
            }

            return firstLine;

        } catch (Exception e) {
            log.error("Failed to extract invoice line item from Stripe event {}", event.getId(), e);
            throw new InternalErrorException("Failed to extract invoice line item from Stripe event " + event.getId());
        }
    }

    private LocalDate toLocalDate(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
    }
}