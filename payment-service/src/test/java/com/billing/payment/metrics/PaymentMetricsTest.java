package com.billing.payment.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final PaymentMetrics paymentMetrics = new PaymentMetrics(registry);

    @Test
    void recordProcessPaymentQueueMessageConsumedTotal_shouldIncrementSuccessCounter_whenCalled() {
        paymentMetrics.recordProcessPaymentQueueMessageConsumedTotal();

        assertThat(registry.get("billing.payments.sqs.received.total")
                .tag("status", "success").tag("queue", "process-payment-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordProcessPaymentQueueMessageConsumptionFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        paymentMetrics.recordProcessPaymentQueueMessageConsumptionFailedTotal();

        assertThat(registry.get("billing.payments.sqs.failure.total")
                .tag("status", "failure").tag("queue", "process-payment-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentOutcomeSnsPublishedTotal_shouldTagApprovedTopic_whenOutcomeIsApproved() {
        paymentMetrics.recordPaymentOutcomeSnsPublishedTotal(true);

        assertThat(registry.get("billing.payments.sns.published.total")
                .tag("topic", "payment-approved-topic").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentOutcomeSnsPublishedTotal_shouldTagFailedTopic_whenOutcomeIsNotApproved() {
        paymentMetrics.recordPaymentOutcomeSnsPublishedTotal(false);

        assertThat(registry.get("billing.payments.sns.published.total")
                .tag("topic", "payment-failed-topic").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentOutcomeSnsPublishFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        paymentMetrics.recordPaymentOutcomeSnsPublishFailedTotal(true);

        assertThat(registry.get("billing.payments.sns.failure.total")
                .tag("status", "failure").tag("topic", "payment-approved-topic")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentDlqMessageSentTotal_shouldIncrementSuccessCounter_whenCalled() {
        paymentMetrics.recordPaymentDlqMessageSentTotal();

        assertThat(registry.get("billing.payment.dlq.published.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentDlqMessageSendFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        paymentMetrics.recordPaymentDlqMessageSendFailedTotal();

        assertThat(registry.get("billing.payment.dlq.failure.total")
                .tag("status", "failure").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordStripeSubscriptionChargeSubmittedTotal_shouldIncrementCounter_whenCalled() {
        paymentMetrics.recordStripeSubscriptionChargeSubmittedTotal();

        assertThat(registry.get("billing.payments.stripe.processing.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoicePaidWebhookHandledTotal_shouldIncrementSuccessCounter_whenCalled() {
        paymentMetrics.recordInvoicePaidWebhookHandledTotal();

        assertThat(registry.get("billing.payments.webhook.success.invoice-paid.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoicePaidWebhookHandlingFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        paymentMetrics.recordInvoicePaidWebhookHandlingFailedTotal();

        assertThat(registry.get("billing.payments.webhook.failure.invoice-paid.total")
                .tag("status", "failure").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoicePaymentFailedWebhookHandledTotal_shouldIncrementSuccessCounter_whenCalled() {
        paymentMetrics.recordInvoicePaymentFailedWebhookHandledTotal();

        assertThat(registry.get("billing.payments.webhook.success.invoice-payment-failed.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoicePaymentFailedWebhookHandlingFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        paymentMetrics.recordInvoicePaymentFailedWebhookHandlingFailedTotal();

        assertThat(registry.get("billing.payments.webhook.failure.invoice-payment-failed.total")
                .tag("status", "failure").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentApprovedOutcomeTotal_shouldIncrementCounter_whenCalled() {
        paymentMetrics.recordPaymentApprovedOutcomeTotal();

        assertThat(registry.get("billing.payments.approved.total")
                .tag("payment-status", "approved").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentFailedOutcomeTotal_shouldIncrementCounter_whenCalled() {
        paymentMetrics.recordPaymentFailedOutcomeTotal();

        assertThat(registry.get("billing.payments.failed.total")
                .tag("payment-status", "failed").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordStripeWebhookRequestHandledTotal_shouldIncrementSuccessCounter_whenCalled() {
        paymentMetrics.recordStripeWebhookRequestHandledTotal();

        assertThat(registry.get("billing.payments.stripe.webhook.received.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordStripeWebhookRequestHandlingFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        paymentMetrics.recordStripeWebhookRequestHandlingFailedTotal();

        assertThat(registry.get("billing.payments.stripe.webhook.failed.total")
                .tag("status", "failure").counter().count()).isEqualTo(1.0);
    }
}
