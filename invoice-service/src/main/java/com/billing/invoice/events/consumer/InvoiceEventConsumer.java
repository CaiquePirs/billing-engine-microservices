package com.billing.invoice.events.consumer;

import com.billing.invoice.advice.exceptions.InternalErrorException;
import com.billing.invoice.events.data.SnsMessage;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.metrics.InvoiceMetrics;
import com.billing.invoice.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventConsumer {

    private final ObjectMapper objectMapper;
    private final InvoiceService invoiceService;
    private final InvoiceMetrics invoiceMetrics;

    @SqsListener("${aws.sqs.queue.generate-invoice}")
    public void generateNewInvoiceQueue(SnsMessage snsMessage) {
        try {
            SubscriptionPaymentEvent event = objectMapper.readValue(
                    snsMessage.Message(),
                    SubscriptionPaymentEvent.class);

            invoiceService.generateInvoice(event);
            invoiceMetrics.recordGenerateInvoiceQueueMessageConsumedTotal();

        } catch (Exception e) {
            log.error("Failed to process invoice generation event from SQS. Raw message: {}", snsMessage.Message(), e);
            invoiceMetrics.recordGenerateInvoiceQueueMessageConsumptionFailedTotal();
            throw new InternalErrorException("Failed to generate invoice from incoming payment-approved SQS event.", e);
        }
    }
}
