package com.billing.invoice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceMetrics {

    private final MeterRegistry meterRegistry;

    public void recordGenerateInvoiceQueueMessageConsumedTotal() {
        Counter.builder("invoice.sqs.received.total")
                .description("Total messages successfully consumed from the generate-invoice SQS queue")
                .tag("status", "success")
                .tag("queue", "generate-invoice-queue")
                .register(meterRegistry)
                .increment();
    }

    public void recordGenerateInvoiceQueueMessageConsumptionFailedTotal() {
        Counter.builder("invoice.sqs.received.total")
                .description("Total messages that failed deserialization or processing from the generate-invoice SQS queue")
                .tag("status", "failure")
                .tag("queue", "generate-invoice-queue")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoiceCreatedQueueMessageSentTotal() {
        Counter.builder("invoice.sqs.published.total")
                .description("Total invoice-created events successfully published to the invoice-created SQS queue")
                .tag("status", "success")
                .tag("queue", "invoice-created-queue")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoiceCreatedQueueMessageSendFailedTotal() {
        Counter.builder("invoice.sqs.published.total")
                .description("Total invoice-created events that failed to publish to the invoice-created SQS queue")
                .tag("status", "failure")
                .tag("queue", "invoice-created-queue")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoiceGeneratedTotal() {
        Counter.builder("invoice.generated.total")
                .description("Total invoices successfully generated (PDF built and persisted)")
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoiceGenerationFailedTotal() {
        Counter.builder("invoice.generated.total")
                .description("Total invoice generation attempts that failed before the invoice could be persisted")
                .tag("status", "failure")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoicePdfUploadedToS3Total() {
        Counter.builder("invoice.s3.upload.total")
                .description("Total invoice PDFs successfully uploaded to S3")
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoicePdfUploadToS3FailedTotal() {
        Counter.builder("invoice.s3.upload.total")
                .description("Total invoice PDF uploads to S3 that failed")
                .tag("status", "failure")
                .register(meterRegistry)
                .increment();
    }
}
