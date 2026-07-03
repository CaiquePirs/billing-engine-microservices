package com.authentication.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationMetrics {

    private final MeterRegistry registry;

    public void recordSignup() {
        Counter.builder("authentication.signup")
                .description("Number of customer signups")
                .register(registry)
                .increment();
    }

    public void recordLogin(boolean success) {
        Counter.builder("authentication.login")
                .description("Number of login attempts")
                .tag("result", success ? "success" : "failure")
                .register(registry)
                .increment();
    }
}
