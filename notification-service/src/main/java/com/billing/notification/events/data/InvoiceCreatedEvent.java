package com.billing.invoice.events.data;

import com.billing.invoice.model.InvoiceStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record InvoiceCreatedEvent(
        UUID invoiceId,
        String s3Key,
        UUID subscriptionId,
        InvoiceStatus status,
        String paymentStatus,
        String subscriptionStatus,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        PlanResponseDTO plan,
        CustomerClientResponse customer) {
}
