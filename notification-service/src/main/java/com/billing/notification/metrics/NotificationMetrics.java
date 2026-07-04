package com.billing.notification.metrics;

import com.billing.notification.model.NotificationTemplate;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public void recordSubscriptionCreatedEmailSentTotal() {
        recordEmailSentByEventTotal(NotificationTemplate.SUBSCRIPTION_CREATED);
    }

    public void recordPaymentApprovedEmailSentTotal() {
        recordEmailSentByEventTotal(NotificationTemplate.SUBSCRIPTION_PAID);
    }

    public void recordPaymentFailedEmailSentTotal() {
        recordEmailSentByEventTotal(NotificationTemplate.SUBSCRIPTION_CANCELLED);
    }

    public void recordInvoiceCreatedEmailSentTotal() {
        recordEmailSentByEventTotal(NotificationTemplate.INVOICE_CREATED);
    }

    private void recordEmailSentByEventTotal(NotificationTemplate template) {
        Counter.builder("notification.email.sent.total")
                .description("Total emails sent, tagged by the originating business event")
                .tag("event", template.name())
                .register(meterRegistry)
                .increment();
    }

    public void recordNewSubscriptionQueueMessageConsumedTotal() {
        recordSqsMessageConsumedTotal("notify-new-subscription-queue");
    }

    public void recordNewSubscriptionQueueMessageConsumptionFailedTotal() {
        recordSqsMessageConsumptionFailedTotal("notify-new-subscription-queue");
    }

    public void recordPaymentApprovedQueueMessageConsumedTotal() {
        recordSqsMessageConsumedTotal("notify-payment-approved-queue");
    }

    public void recordPaymentApprovedQueueMessageConsumptionFailedTotal() {
        recordSqsMessageConsumptionFailedTotal("notify-payment-approved-queue");
    }

    public void recordPaymentFailedQueueMessageConsumedTotal() {
        recordSqsMessageConsumedTotal("notify-payment-failed-queue");
    }

    public void recordPaymentFailedQueueMessageConsumptionFailedTotal() {
        recordSqsMessageConsumptionFailedTotal("notify-payment-failed-queue");
    }

    public void recordInvoiceCreatedQueueMessageConsumedTotal() {
        recordSqsMessageConsumedTotal("invoice-created-queue");
    }

    public void recordInvoiceCreatedQueueMessageConsumptionFailedTotal() {
        recordSqsMessageConsumptionFailedTotal("invoice-created-queue");
    }

    private void recordSqsMessageConsumedTotal(String queue) {
        Counter.builder("notification.sqs.received.total")
                .description("Total messages successfully consumed from SQS, tagged by queue")
                .tag("status", "success")
                .tag("queue", queue)
                .register(meterRegistry)
                .increment();
    }

    private void recordSqsMessageConsumptionFailedTotal(String queue) {
        Counter.builder("notification.sqs.received.total")
                .description("Total messages that failed deserialization or processing from SQS, tagged by queue")
                .tag("status", "failure")
                .tag("queue", queue)
                .register(meterRegistry)
                .increment();
    }

    public void recordEmailWithAttachmentSentTotal() {
        Counter.builder("notification.email.attachment.sent.total")
                .description("Total emails successfully sent with a file attachment (e.g. invoice PDF)")
                .register(meterRegistry)
                .increment();
    }

    public void recordEmailDeliverySucceededTotal() {
        Counter.builder("notification.email.delivery.total")
                .description("Total email delivery attempts via SES, regardless of business event, tagged by outcome")
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    public void recordEmailDeliveryFailedTotal() {
        Counter.builder("notification.email.delivery.total")
                .description("Total email delivery attempts via SES, regardless of business event, tagged by outcome")
                .tag("status", "failure")
                .register(meterRegistry)
                .increment();
    }
}
