package com.billing.subscriptions.metrics;

import com.billing.subscriptions.model.enums.SubscriptionStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BillingSubscriptionMetrics {

    private final MeterRegistry meterRegistry;

    public void recordSubscriptionCreatedTotal() {
        recordSubscriptionStatusChangeTotal(SubscriptionStatus.PENDING.toString());
    }

    public void recordSubscriptionActivatedTotal(){
        recordSubscriptionStatusChangeTotal(SubscriptionStatus.ACTIVE.toString());
    }

    public void recordSubscriptionCancelledTotal(){
        recordSubscriptionStatusChangeTotal(SubscriptionStatus.CANCELED.toString());
    }

    private void recordSubscriptionStatusChangeTotal(String status) {
        Counter.builder("billing.subscription.status.total")
                .description("Total subscription lifecycle transitions, tagged by the resulting status")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    public void recordPlanCreatedTotal(String planName) {
        Counter.builder("billing.plan.created")
                .description("Total plans created")
                .tag("plan", planName)
                .register(meterRegistry)
                .increment();
    }

    public void recordSubscriptionCreatedSnsPublishedTotal() {
        Counter.builder("billing.subscriptions.sns.published.total")
                .description("Total subscription-created events successfully published to SNS")
                .tag("status", "success")
                .tag("topic", "subscription-created")
                .register(meterRegistry)
                .increment();
    }

    public void recordSubscriptionCreatedSnsPublishFailedTotal() {
        Counter.builder("billing.subscriptions.sns.failure.total")
                .description("Total subscription-created events that failed to publish to SNS")
                .tag("status", "failure")
                .tag("topic", "subscription-created")
                .register(meterRegistry)
                .increment();
    }

    public void recordActiveSubscriptionQueueMessageConsumedTotal() {
        Counter.builder("billing.subscriptions.sqs.received.total")
                .description("Total messages successfully consumed from the active-subscription SQS queue")
                .tag("status", "success")
                .tag("queue", "active-subscription-queue")
                .register(meterRegistry)
                .increment();
    }

    public void recordDeactivateSubscriptionQueueConsumedTotal() {
        Counter.builder("billing.subscriptions.sqs.received.total")
                .description("Total messages successfully consumed from the deactivate-subscription SQS queue")
                .tag("status", "success")
                .tag("queue", "deactivate-subscription-queue")
                .register(meterRegistry)
                .increment();
    }

    public void recordDeactivateSubscriptionQueueMessageFailedTotal() {
        Counter.builder("billing.subscriptions.sqs.failure.total")
                .description("Total messages that failed to be consumed from the deactivate-subscription SQS queue")
                .tag("status", "failure")
                .tag("queue", "desactivate-subscription-queue")
                .register(meterRegistry)
                .increment();
    }
}
