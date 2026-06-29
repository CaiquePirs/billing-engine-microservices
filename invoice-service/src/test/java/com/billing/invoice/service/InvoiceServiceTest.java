package com.billing.invoice.service;

import com.billing.invoice.events.data.CustomerAddressResponse;
import com.billing.invoice.events.data.CustomerClientResponse;
import com.billing.invoice.events.data.PlanResponseDTO;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.events.publisher.InvoiceEventPublisher;
import com.billing.invoice.mapper.InvoiceMapper;
import com.billing.invoice.model.Invoice;
import com.billing.invoice.model.InvoiceStatus;
import com.billing.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoicePdfGenerator invoicePdfGenerator;
    @Mock private InvoiceStorageService invoiceStorageService;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private InvoiceEventPublisher invoiceEventPublisher;

    @InjectMocks
    private InvoiceService invoiceService;

    private SubscriptionPaymentEvent buildPaymentEvent() {
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        CustomerAddressResponse address = new CustomerAddressResponse(
                UUID.randomUUID(), "Main St", "10", "Dublin", "Leinster", "Ireland", "D01 AB12");

        CustomerClientResponse customer = new CustomerClientResponse(
                UUID.randomUUID(), "John", "Doe", "john@example.com", "+35312345678", address, "cus_abc123");

        PlanResponseDTO plan = new PlanResponseDTO(
                UUID.randomUUID(), "Premium", "Premium Plan", new BigDecimal("9900"),
                "EUR", "month", "price_abc123", true);

        return new SubscriptionPaymentEvent(paymentId, subscriptionId, "APPROVED",
                "ACTIVE", LocalDate.now(), LocalDate.now().plusMonths(1), plan, customer);
    }

    private Invoice buildInvoice(UUID invoiceId, UUID subscriptionId) {
        return Invoice.builder()
                .id(invoiceId)
                .subscriptionId(subscriptionId)
                .paymentId(UUID.randomUUID())
                .amount(9900L)
                .currency("EUR")
                .invoiceStatus(InvoiceStatus.GENERATED)
                .s3Key("invoices/invoice-" + invoiceId + ".pdf")
                .dueDate(LocalDate.now().plusMonths(1))
                .build();
    }

    @Test
    void generateInvoice_shouldGeneratePdfUploadAndPublishEvent_whenEventIsValid() {
        SubscriptionPaymentEvent event = buildPaymentEvent();
        byte[] pdfBytes = "pdf-content".getBytes();
        String s3Key = "invoices/invoice-abc.pdf";
        Invoice invoice = buildInvoice(UUID.randomUUID(), event.subscriptionId());

        when(invoicePdfGenerator.generate(any(UUID.class), eq(event))).thenReturn(pdfBytes);
        when(invoiceStorageService.uploadInvoicePdf(event.paymentId(), pdfBytes)).thenReturn(s3Key);
        when(invoiceMapper.buildInvoice(any(UUID.class), eq(event), eq(s3Key))).thenReturn(invoice);
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        doNothing().when(invoiceEventPublisher).publishInvoiceCreatedEvent(invoice, event);

        invoiceService.generateInvoice(event);

        verify(invoicePdfGenerator).generate(any(UUID.class), eq(event));
        verify(invoiceStorageService).uploadInvoicePdf(event.paymentId(), pdfBytes);
        verify(invoiceRepository).save(invoice);
        verify(invoiceEventPublisher).publishInvoiceCreatedEvent(invoice, event);
    }

    @Test
    void generateInvoice_shouldNotSaveInvoice_whenPdfGenerationFails() {
        SubscriptionPaymentEvent event = buildPaymentEvent();
        when(invoicePdfGenerator.generate(any(UUID.class), eq(event)))
                .thenThrow(new RuntimeException("PDF generation failed"));

        assertThatThrownBy(() -> invoiceService.generateInvoice(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PDF generation failed");

        verify(invoiceRepository, never()).save(any());
        verify(invoiceEventPublisher, never()).publishInvoiceCreatedEvent(any(), any());
    }

    @Test
    void generateInvoice_shouldNotPublishEvent_whenStorageFails() {
        SubscriptionPaymentEvent event = buildPaymentEvent();
        byte[] pdfBytes = "pdf-content".getBytes();

        when(invoicePdfGenerator.generate(any(UUID.class), eq(event))).thenReturn(pdfBytes);
        when(invoiceStorageService.uploadInvoicePdf(event.paymentId(), pdfBytes))
                .thenThrow(new RuntimeException("S3 upload failed"));

        assertThatThrownBy(() -> invoiceService.generateInvoice(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 upload failed");

        verify(invoiceRepository, never()).save(any());
        verify(invoiceEventPublisher, never()).publishInvoiceCreatedEvent(any(), any());
    }
}
