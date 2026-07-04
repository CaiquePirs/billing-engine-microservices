package com.billing.invoice.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final InvoiceMetrics invoiceMetrics = new InvoiceMetrics(registry);

    @Test
    void recordGenerateInvoiceQueueMessageConsumedTotal_shouldIncrementSuccessCounter_whenCalled() {
        invoiceMetrics.recordGenerateInvoiceQueueMessageConsumedTotal();

        assertThat(registry.get("invoice.sqs.received.total")
                .tag("status", "success").tag("queue", "generate-invoice-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordGenerateInvoiceQueueMessageConsumptionFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        invoiceMetrics.recordGenerateInvoiceQueueMessageConsumptionFailedTotal();

        assertThat(registry.get("invoice.sqs.received.total")
                .tag("status", "failure").tag("queue", "generate-invoice-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoiceCreatedQueueMessageSentTotal_shouldIncrementSuccessCounter_whenCalled() {
        invoiceMetrics.recordInvoiceCreatedQueueMessageSentTotal();

        assertThat(registry.get("invoice.sqs.published.total")
                .tag("status", "success").tag("queue", "invoice-created-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoiceCreatedQueueMessageSendFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        invoiceMetrics.recordInvoiceCreatedQueueMessageSendFailedTotal();

        assertThat(registry.get("invoice.sqs.published.total")
                .tag("status", "failure").tag("queue", "invoice-created-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoiceGeneratedTotal_shouldIncrementSuccessCounter_whenCalled() {
        invoiceMetrics.recordInvoiceGeneratedTotal();

        assertThat(registry.get("invoice.generated.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoiceGenerationFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        invoiceMetrics.recordInvoiceGenerationFailedTotal();

        assertThat(registry.get("invoice.generated.total")
                .tag("status", "failure").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoicePdfUploadedToS3Total_shouldIncrementSuccessCounter_whenCalled() {
        invoiceMetrics.recordInvoicePdfUploadedToS3Total();

        assertThat(registry.get("invoice.s3.upload.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoicePdfUploadToS3FailedTotal_shouldIncrementFailureCounter_whenCalled() {
        invoiceMetrics.recordInvoicePdfUploadToS3FailedTotal();

        assertThat(registry.get("invoice.s3.upload.total")
                .tag("status", "failure").counter().count()).isEqualTo(1.0);
    }
}
