package com.billing.invoice.service;

import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.model.Invoice;
import com.billing.invoice.model.InvoiceStatus;
import com.billing.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePdfGenerator invoicePdfGenerator;
    private final InvoiceStorageService invoiceStorageService;

    @Transactional
    public void generateInvoice(SubscriptionPaymentEvent event) {
        UUID invoiceId = UUID.randomUUID();

        byte[] pdfBytes = invoicePdfGenerator.generate(event.paymentId(), event);
        String s3Key = invoiceStorageService.uploadInvoicePdf(event.paymentId(), pdfBytes);

        Invoice invoice = buildInvoice(invoiceId, event, s3Key);
        invoiceRepository.save(invoice);
    }

    private Invoice buildInvoice(UUID invoiceId, SubscriptionPaymentEvent event, String s3Key) {
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
}
