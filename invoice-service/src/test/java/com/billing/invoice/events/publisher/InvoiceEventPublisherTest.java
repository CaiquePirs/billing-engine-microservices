package com.billing.invoice.events.publisher;

import com.billing.invoice.advice.exceptions.InternalErrorException;
import com.billing.invoice.events.data.CustomerAddressResponse;
import com.billing.invoice.events.data.CustomerClientResponse;
import com.billing.invoice.events.data.InvoiceCreatedEvent;
import com.billing.invoice.events.data.PlanResponseDTO;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.mapper.InvoiceMapper;
import com.billing.invoice.model.Invoice;
import com.billing.invoice.model.InvoiceStatus;
import com.billing.invoice.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceEventPublisherTest {

    @Mock private InvoiceMapper invoiceMapper;
    @Mock private ObjectMapper objectMapper;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private SqsClient sqsClient;

    @InjectMocks
    private InvoiceEventPublisher invoiceEventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invoiceEventPublisher, "invoiceCreatedQueue",
                "https://sqs.us-east-1.amazonaws.com/000000000000/invoice-created-queue");
    }

    private Invoice buildInvoice() {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .amount(9900L)
                .currency("EUR")
                .invoiceStatus(InvoiceStatus.GENERATED)
                .s3Key("invoices/test.pdf")
                .dueDate(LocalDate.now().plusMonths(1))
                .build();
    }

    private SubscriptionPaymentEvent buildPaymentEvent() {
        CustomerAddressResponse address = new CustomerAddressResponse(
                UUID.randomUUID(), "Main St", "10", "Dublin", "Leinster", "Ireland", "D01 AB12");
        CustomerClientResponse customer = new CustomerClientResponse(
                UUID.randomUUID(), "John", "Doe", "john@example.com", "+35312345678", address, "cus_abc123");
        PlanResponseDTO plan = new PlanResponseDTO(
                UUID.randomUUID(), "Premium", "Premium Plan", new BigDecimal("9900"),
                "EUR", "month", "price_abc123", true);
        return new SubscriptionPaymentEvent(UUID.randomUUID(), UUID.randomUUID(), "APPROVED",
                "ACTIVE", LocalDate.now(), LocalDate.now().plusMonths(1), plan, customer);
    }

    @Test
    void publishInvoiceCreatedEvent_shouldSendToSqsAndUpdateStatusToSent_whenSucceeds() throws Exception {
        Invoice invoice = buildInvoice();
        SubscriptionPaymentEvent event = buildPaymentEvent();
        InvoiceCreatedEvent invoiceEvent = mock(InvoiceCreatedEvent.class);

        when(invoiceMapper.mapToInvoiceCreatedEvent(invoice, event)).thenReturn(invoiceEvent);
        when(objectMapper.writeValueAsString(invoiceEvent)).thenReturn("{\"invoiceId\":\"some-id\"}");
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        invoiceEventPublisher.publishInvoiceCreatedEvent(invoice, event);

        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
        assertThat(invoice.getInvoiceStatus()).isEqualTo(InvoiceStatus.SENT);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void publishInvoiceCreatedEvent_shouldThrowInternalErrorException_whenSqsClientFails() throws Exception {
        Invoice invoice = buildInvoice();
        SubscriptionPaymentEvent event = buildPaymentEvent();
        InvoiceCreatedEvent invoiceEvent = mock(InvoiceCreatedEvent.class);

        when(invoiceMapper.mapToInvoiceCreatedEvent(invoice, event)).thenReturn(invoiceEvent);
        when(objectMapper.writeValueAsString(invoiceEvent)).thenReturn("{\"invoiceId\":\"some-id\"}");
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(new RuntimeException("SQS error"));

        assertThatThrownBy(() -> invoiceEventPublisher.publishInvoiceCreatedEvent(invoice, event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Failed to publish invoice-created SQS event");

        verify(invoiceRepository, never()).save(invoice);
    }

    @Test
    void publishInvoiceCreatedEvent_shouldThrowInternalErrorException_whenSerializationFails() throws Exception {
        Invoice invoice = buildInvoice();
        SubscriptionPaymentEvent event = buildPaymentEvent();
        InvoiceCreatedEvent invoiceEvent = mock(InvoiceCreatedEvent.class);

        when(invoiceMapper.mapToInvoiceCreatedEvent(invoice, event)).thenReturn(invoiceEvent);
        when(objectMapper.writeValueAsString(invoiceEvent)).thenThrow(new RuntimeException("Serialization error"));

        assertThatThrownBy(() -> invoiceEventPublisher.publishInvoiceCreatedEvent(invoice, event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Failed to publish invoice-created SQS event");
    }
}
