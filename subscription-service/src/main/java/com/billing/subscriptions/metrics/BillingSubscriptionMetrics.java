package com.billing.subscriptions.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BillingSubscriptionMetrics {

    private final MeterRegistry meterRegistry;

    public void recordSubscriptionCreated(String status) {
        Counter.builder("billing.subscription.created")
                .description("Number of subscriptions created")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    public void recordSubscriptionPlanCreated(String planName) {
        Counter.builder("billing.plan.created")
                .description("Number of plans created")
                .tag("plan", planName)
                .register(meterRegistry)
                .increment();
    }
}
