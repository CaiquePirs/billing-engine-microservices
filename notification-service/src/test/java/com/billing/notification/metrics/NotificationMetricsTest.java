package com.billing.notification.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final NotificationMetrics notificationMetrics = new NotificationMetrics(registry);

    @Test
    void recordSubscriptionCreatedEmailSentTotal_shouldIncrementCounterWithSubscriptionCreatedEventTag_whenCalled() {
        notificationMetrics.recordSubscriptionCreatedEmailSentTotal();

        assertThat(registry.get("notification.email.sent.total")
                .tag("event", "SUBSCRIPTION_CREATED").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentApprovedEmailSentTotal_shouldIncrementCounterWithSubscriptionPaidEventTag_whenCalled() {
        notificationMetrics.recordPaymentApprovedEmailSentTotal();

        assertThat(registry.get("notification.email.sent.total")
                .tag("event", "SUBSCRIPTION_PAID").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentFailedEmailSentTotal_shouldIncrementCounterWithSubscriptionCancelledEventTag_whenCalled() {
        notificationMetrics.recordPaymentFailedEmailSentTotal();

        assertThat(registry.get("notification.email.sent.total")
                .tag("event", "SUBSCRIPTION_CANCELLED").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoiceCreatedEmailSentTotal_shouldIncrementCounterWithInvoiceCreatedEventTag_whenCalled() {
        notificationMetrics.recordInvoiceCreatedEmailSentTotal();

        assertThat(registry.get("notification.email.sent.total")
                .tag("event", "INVOICE_CREATED").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordEmailSentByEvent_shouldTagCountersSeparately_whenEventDiffers() {
        notificationMetrics.recordSubscriptionCreatedEmailSentTotal();
        notificationMetrics.recordInvoiceCreatedEmailSentTotal();

        assertThat(registry.get("notification.email.sent.total").tag("event", "SUBSCRIPTION_CREATED").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("notification.email.sent.total").tag("event", "INVOICE_CREATED").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordNewSubscriptionQueueMessageConsumedTotal_shouldIncrementSuccessCounter_whenCalled() {
        notificationMetrics.recordNewSubscriptionQueueMessageConsumedTotal();

        assertThat(registry.get("notification.sqs.received.total")
                .tag("status", "success").tag("queue", "notify-new-subscription-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordNewSubscriptionQueueMessageConsumptionFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        notificationMetrics.recordNewSubscriptionQueueMessageConsumptionFailedTotal();

        assertThat(registry.get("notification.sqs.received.total")
                .tag("status", "failure").tag("queue", "notify-new-subscription-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentApprovedQueueMessageConsumedTotal_shouldIncrementSuccessCounter_whenCalled() {
        notificationMetrics.recordPaymentApprovedQueueMessageConsumedTotal();

        assertThat(registry.get("notification.sqs.received.total")
                .tag("status", "success").tag("queue", "notify-payment-approved-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentApprovedQueueMessageConsumptionFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        notificationMetrics.recordPaymentApprovedQueueMessageConsumptionFailedTotal();

        assertThat(registry.get("notification.sqs.received.total")
                .tag("status", "failure").tag("queue", "notify-payment-approved-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentFailedQueueMessageConsumedTotal_shouldIncrementSuccessCounter_whenCalled() {
        notificationMetrics.recordPaymentFailedQueueMessageConsumedTotal();

        assertThat(registry.get("notification.sqs.received.total")
                .tag("status", "success").tag("queue", "notify-payment-failed-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPaymentFailedQueueMessageConsumptionFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        notificationMetrics.recordPaymentFailedQueueMessageConsumptionFailedTotal();

        assertThat(registry.get("notification.sqs.received.total")
                .tag("status", "failure").tag("queue", "notify-payment-failed-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoiceCreatedQueueMessageConsumedTotal_shouldIncrementSuccessCounter_whenCalled() {
        notificationMetrics.recordInvoiceCreatedQueueMessageConsumedTotal();

        assertThat(registry.get("notification.sqs.received.total")
                .tag("status", "success").tag("queue", "invoice-created-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordInvoiceCreatedQueueMessageConsumptionFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        notificationMetrics.recordInvoiceCreatedQueueMessageConsumptionFailedTotal();

        assertThat(registry.get("notification.sqs.received.total")
                .tag("status", "failure").tag("queue", "invoice-created-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordEmailWithAttachmentSentTotal_shouldIncrementCounter_whenCalled() {
        notificationMetrics.recordEmailWithAttachmentSentTotal();

        assertThat(registry.get("notification.email.attachment.sent.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordEmailDeliverySucceededTotal_shouldIncrementSuccessCounter_whenCalled() {
        notificationMetrics.recordEmailDeliverySucceededTotal();

        assertThat(registry.get("notification.email.delivery.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordEmailDeliveryFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        notificationMetrics.recordEmailDeliveryFailedTotal();

        assertThat(registry.get("notification.email.delivery.total")
                .tag("status", "failure").counter().count()).isEqualTo(1.0);
    }
}
