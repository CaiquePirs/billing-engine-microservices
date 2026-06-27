package com.billing.invoice.events.publisher;

import com.billing.invoice.advice.exceptions.InternalErrorException;
import com.billing.invoice.events.data.InvoiceCreatedEvent;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.mapper.InvoiceMapper;
import com.billing.invoice.model.Invoice;
import com.billing.invoice.model.InvoiceStatus;
import com.billing.invoice.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventPublisher {

    private final InvoiceMapper invoiceMapper;
    private final ObjectMapper objectMapper;
    private final InvoiceRepository invoiceRepository;
    private final SqsClient sqsClient;

    @Value("${INVOICE_CREATED_QUEUE}")
    private String invoiceCreatedQueue;

    public void publishInvoiceCreatedEvent(Invoice invoice, SubscriptionPaymentEvent event) {
        InvoiceCreatedEvent invoiceEvent = invoiceMapper.mapToInvoiceCreatedEvent(invoice, event);

        try {
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(invoiceCreatedQueue)
                    .messageBody(objectMapper.writeValueAsString(invoiceEvent))
                    .build());

            invoice.setInvoiceStatus(InvoiceStatus.SENT);
            invoiceRepository.save(invoice);

        } catch (Exception e) {
            log.error("Failed to publish invoice-created event for invoice ID {}", invoice.getId(), e);
            throw new InternalErrorException("Failed to publish invoice-created SQS event for invoice ID: " + invoice.getId());
        }
    }
}
