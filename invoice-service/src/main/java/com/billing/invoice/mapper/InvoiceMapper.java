package com.billing.invoice.mapper;

import com.billing.invoice.events.data.InvoiceCreatedEvent;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.model.Invoice;
import com.billing.invoice.model.InvoiceStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InvoiceMapper {

    public Invoice buildInvoice(UUID invoiceId, SubscriptionPaymentEvent event, String s3Key) {
        long amountInCents = event.plan().price().longValue();

        return Invoice.builder()
                .id(invoiceId)
                .subscriptionId(event.subscriptionId())
                .paymentId(event.paymentId())
                .amount(amountInCents)
                .currency(event.plan().currency().toUpperCase())
                .invoiceStatus(InvoiceStatus.GENERATED)
                .s3Key(s3Key)
                .dueDate(event.currentPeriodEnd())
                .build();
    }

    public InvoiceCreatedEvent mapToInvoiceCreatedEvent(Invoice invoice, SubscriptionPaymentEvent event) {
        return InvoiceCreatedEvent.builder()
                .invoiceId(invoice.getId())
                .s3Key(invoice.getS3Key())
                .subscriptionId(invoice.getSubscriptionId())
                .paymentStatus(event.paymentStatus())
                .subscriptionStatus(event.subscriptionStatus())
                .currentPeriodStart(event.currentPeriodStart())
                .currentPeriodEnd(event.currentPeriodEnd())
                .customer(event.customer())
                .plan(event.plan())
                .status(InvoiceStatus.GENERATED.toString())
                .build();
    }
}
