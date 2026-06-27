package com.billing.invoice.service;

import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.events.publisher.InvoiceEventPublisher;
import com.billing.invoice.mapper.InvoiceMapper;
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
    private final InvoiceMapper invoiceMapper;
    private final InvoiceEventPublisher invoiceEventPublisher;

    @Transactional
    public void generateInvoice(SubscriptionPaymentEvent event) {
        UUID invoiceId = UUID.randomUUID();

        byte[] pdfBytes = invoicePdfGenerator.generate(invoiceId, event);
        String s3Key = invoiceStorageService.uploadInvoicePdf(event.paymentId(), pdfBytes);

        Invoice invoice = invoiceMapper.buildInvoice(invoiceId, event, s3Key);
        invoiceRepository.save(invoice);

        invoiceEventPublisher.publishInvoiceCreatedEvent(invoice, event);
    }
}
