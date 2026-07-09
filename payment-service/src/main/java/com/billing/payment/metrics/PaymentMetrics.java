package com.billing.payment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;

    public void recordProcessPaymentQueueMessageConsumedTotal() {
        Counter.builder("billing.payments.sqs.received.total")
                .description("Total messages successfully consumed from the process-payment SQS queue")
                .tag("status", "success")
                .tag("queue", "process-payment-queue")
                .register(meterRegistry)
                .increment();
    }

    public void recordProcessPaymentQueueMessageConsumptionFailedTotal() {
        Counter.builder("billing.payments.sqs.failure.total")
                .description("Total messages that failed deserialization or processing from the process-payment SQS queue")
                .tag("status", "failure")
                .tag("queue", "process-payment-queue")
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentOutcomeSnsPublishedTotal(boolean isApprovedOutcome) {
        Counter.builder("billing.payments.sns.published.total")
                .description("Total payment outcome events (approved or failed) successfully published to SNS")
                .tag("status", "success")
                .tag("topic", isApprovedOutcome ? "payment-approved-topic" : "payment-failed-topic")
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentOutcomeSnsPublishFailedTotal(boolean isApprovedOutcome) {
        Counter.builder("billing.payments.sns.failure.total")
                .description("Total payment outcome events that failed to publish to SNS")
                .tag("status", "failure")
                .tag("topic", isApprovedOutcome ? "payment-approved-topic" : "payment-failed-topic")
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentDlqMessageSentTotal() {
        Counter.builder("billing.payment.dlq.published.total")
                .description("Total payment events successfully forwarded to the dead-letter queue after processing failure")
                .tag("status", "success")
                .tag("queue", "payment-processing-dlq")
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentDlqMessageSendFailedTotal() {
        Counter.builder("billing.payment.dlq.failure.total")
                .description("Total payment events that failed to be forwarded to the dead-letter queue")
                .tag("status", "failure")
                .tag("queue", "payment-processing-dlq")
                .register(meterRegistry)
                .increment();
    }

    public void recordStripeSubscriptionChargeSubmittedTotal() {
        Counter.builder("billing.payments.stripe.processing.total")
                .description("Total payments submitted to Stripe to create the subscription charge")
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoicePaidWebhookHandledTotal() {
        Counter.builder("billing.payments.webhook.success.invoice-paid.total")
                .description("Total Stripe invoice.paid webhook events successfully handled")
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoicePaidWebhookHandlingFailedTotal() {
        Counter.builder("billing.payments.webhook.failure.invoice-paid.total")
                .description("Total failures handling Stripe invoice.paid webhook events")
                .tag("status", "failure")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoicePaymentFailedWebhookHandledTotal() {
        Counter.builder("billing.payments.webhook.success.invoice-payment-failed.total")
                .description("Total Stripe invoice.payment_failed webhook events successfully handled")
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    public void recordInvoicePaymentFailedWebhookHandlingFailedTotal() {
        Counter.builder("billing.payments.webhook.failure.invoice-payment-failed.total")
                .description("Total failures handling Stripe invoice.payment_failed webhook events")
                .tag("status", "failure")
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentApprovedOutcomeTotal() {
        Counter.builder("billing.payments.approved.total")
                .description("Total payments whose final outcome was approved")
                .tag("payment-status", "approved")
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentFailedOutcomeTotal() {
        Counter.builder("billing.payments.failed.total")
                .description("Total payments whose final outcome was failed")
                .tag("payment-status", "failed")
                .register(meterRegistry)
                .increment();
    }

    public void recordStripeWebhookRequestHandledTotal() {
        Counter.builder("billing.payments.stripe.webhook.received.total")
                .description("Total Stripe webhook HTTP requests successfully verified and processed")
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    public void recordStripeWebhookRequestHandlingFailedTotal() {
        Counter.builder("billing.payments.stripe.webhook.failed.total")
                .description("Total Stripe webhook HTTP requests that failed signature verification or processing")
                .tag("status", "failure")
                .register(meterRegistry)
                .increment();
    }

    public void recordStripeWebhookDuplicateIgnoredTotal() {
        Counter.builder("billing.payments.stripe.webhook.duplicate.total")
                .description("Total Stripe webhook events skipped because they were already processed (idempotency)")
                .tag("status", "duplicate")
                .register(meterRegistry)
                .increment();
    }

}
