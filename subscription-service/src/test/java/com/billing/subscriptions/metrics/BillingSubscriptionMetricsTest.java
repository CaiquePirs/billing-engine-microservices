package com.billing.subscriptions.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillingSubscriptionMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BillingSubscriptionMetrics billingSubscriptionMetrics = new BillingSubscriptionMetrics(registry);

    @Test
    void recordSubscriptionCreatedTotal_shouldIncrementCounterWithPendingStatusTag_whenCalled() {
        billingSubscriptionMetrics.recordSubscriptionCreatedTotal();

        assertThat(registry.get("billing.subscription.status.total").tag("status", "PENDING").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordSubscriptionActivatedTotal_shouldIncrementCounterWithActiveStatusTag_whenCalled() {
        billingSubscriptionMetrics.recordSubscriptionActivatedTotal();

        assertThat(registry.get("billing.subscription.status.total").tag("status", "ACTIVE").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordSubscriptionCancelledTotal_shouldIncrementCounterWithCanceledStatusTag_whenCalled() {
        billingSubscriptionMetrics.recordSubscriptionCancelledTotal();

        assertThat(registry.get("billing.subscription.status.total").tag("status", "CANCELED").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordSubscriptionLifecycleMetrics_shouldTagCountersSeparately_whenStatusDiffers() {
        billingSubscriptionMetrics.recordSubscriptionCreatedTotal();
        billingSubscriptionMetrics.recordSubscriptionActivatedTotal();

        assertThat(registry.get("billing.subscription.status.total").tag("status", "PENDING").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("billing.subscription.status.total").tag("status", "ACTIVE").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordPlanCreatedTotal_shouldIncrementCounterWithPlanTag_whenCalled() {
        billingSubscriptionMetrics.recordPlanCreatedTotal("Premium");

        assertThat(registry.get("billing.plan.created").tag("plan", "Premium").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordSubscriptionCreatedSnsPublishedTotal_shouldIncrementSuccessCounter_whenCalled() {
        billingSubscriptionMetrics.recordSubscriptionCreatedSnsPublishedTotal();

        assertThat(registry.get("billing.subscriptions.sns.published.total")
                .tag("status", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordSubscriptionCreatedSnsPublishFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        billingSubscriptionMetrics.recordSubscriptionCreatedSnsPublishFailedTotal();

        assertThat(registry.get("billing.subscriptions.sns.failure.total")
                .tag("status", "failure").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordActiveSubscriptionQueueMessageConsumedTotal_shouldIncrementSuccessCounter_whenCalled() {
        billingSubscriptionMetrics.recordActiveSubscriptionQueueMessageConsumedTotal();

        assertThat(registry.get("billing.subscriptions.sqs.received.total")
                .tag("status", "success").tag("queue", "active-subscription-queue")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordDeactivateSubscriptionQueueMessageFailedTotal_shouldIncrementFailureCounter_whenCalled() {
        billingSubscriptionMetrics.recordDeactivateSubscriptionQueueMessageFailedTotal();

        assertThat(registry.get("billing.subscriptions.sqs.failure.total")
                .tag("status", "failure").tag("queue", "desactivate-subscription-queue")
                .counter().count()).isEqualTo(1.0);
    }
}
