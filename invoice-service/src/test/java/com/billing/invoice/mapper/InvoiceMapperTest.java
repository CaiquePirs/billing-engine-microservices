package com.billing.invoice.mapper;

import com.billing.invoice.events.data.CustomerAddressResponse;
import com.billing.invoice.events.data.CustomerClientResponse;
import com.billing.invoice.events.data.InvoiceCreatedEvent;
import com.billing.invoice.events.data.PlanResponseDTO;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.model.Invoice;
import com.billing.invoice.model.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceMapperTest {

    private final InvoiceMapper invoiceMapper = new InvoiceMapper();

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

    @Test
    void buildInvoice_shouldMapAllFields_whenEventIsValid() {
        UUID invoiceId = UUID.randomUUID();
        SubscriptionPaymentEvent event = buildPaymentEvent();
        String s3Key = "invoices/invoice-abc.pdf";

        Invoice result = invoiceMapper.buildInvoice(invoiceId, event, s3Key);

        assertThat(result.getId()).isEqualTo(invoiceId);
        assertThat(result.getSubscriptionId()).isEqualTo(event.subscriptionId());
        assertThat(result.getPaymentId()).isEqualTo(event.paymentId());
        assertThat(result.getAmount()).isEqualTo(9900L);
        assertThat(result.getCurrency()).isEqualTo("EUR");
        assertThat(result.getInvoiceStatus()).isEqualTo(InvoiceStatus.GENERATED);
        assertThat(result.getS3Key()).isEqualTo(s3Key);
        assertThat(result.getDueDate()).isEqualTo(event.currentPeriodEnd());
    }

    @Test
    void buildInvoice_shouldSetCurrencyUpperCase_whenCurrencyIsProvided() {
        UUID invoiceId = UUID.randomUUID();
        SubscriptionPaymentEvent event = buildPaymentEvent();

        Invoice result = invoiceMapper.buildInvoice(invoiceId, event, "invoices/test.pdf");

        assertThat(result.getCurrency()).isUpperCase();
    }

    @Test
    void mapToInvoiceCreatedEvent_shouldMapAllFields_whenInvoiceAndEventAreValid() {
        SubscriptionPaymentEvent event = buildPaymentEvent();
        UUID invoiceId = UUID.randomUUID();
        String s3Key = "invoices/invoice-abc.pdf";

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .subscriptionId(event.subscriptionId())
                .paymentId(event.paymentId())
                .amount(9900L)
                .currency("EUR")
                .invoiceStatus(InvoiceStatus.GENERATED)
                .s3Key(s3Key)
                .dueDate(event.currentPeriodEnd())
                .build();

        InvoiceCreatedEvent result = invoiceMapper.mapToInvoiceCreatedEvent(invoice, event);

        assertThat(result.invoiceId()).isEqualTo(invoiceId);
        assertThat(result.s3Key()).isEqualTo(s3Key);
        assertThat(result.subscriptionId()).isEqualTo(event.subscriptionId());
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(result.status()).isEqualTo(InvoiceStatus.GENERATED.toString());
        assertThat(result.customer()).isEqualTo(event.customer());
        assertThat(result.plan()).isEqualTo(event.plan());
    }
}
