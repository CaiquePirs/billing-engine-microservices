package com.billing.subscriptions.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillingSubscriptionMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BillingSubscriptionMetrics billingSubscriptionMetrics = new BillingSubscriptionMetrics(registry);

    @Test
    void recordSubscriptionCreated_shouldIncrementCounterWithStatusTag_whenCalled() {
        billingSubscriptionMetrics.recordSubscriptionCreated("ACTIVE");

        assertThat(registry.get("billing.subscription.created").tag("status", "ACTIVE").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordSubscriptionCreated_shouldTagCountersSeparately_whenStatusDiffers() {
        billingSubscriptionMetrics.recordSubscriptionCreated("ACTIVE");
        billingSubscriptionMetrics.recordSubscriptionCreated("CANCELED");

        assertThat(registry.get("billing.subscription.created").tag("status", "ACTIVE").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("billing.subscription.created").tag("status", "CANCELED").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordSubscriptionPlanCreated_shouldIncrementCounterWithPlanTag_whenCalled() {
        billingSubscriptionMetrics.recordSubscriptionPlanCreated("Premium");

        assertThat(registry.get("billing.plan.created").tag("plan", "Premium").counter().count()).isEqualTo(1.0);
    }
}
