package com.authentication.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AuthenticationMetrics authenticationMetrics = new AuthenticationMetrics(registry);

    @Test
    void recordSignup_shouldIncrementSignupCounter_whenCalled() {
        authenticationMetrics.recordSignup();

        assertThat(registry.get("authentication.signup").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordLogin_shouldIncrementCounterWithSuccessTag_whenLoginSucceeds() {
        authenticationMetrics.recordLogin(true);

        assertThat(registry.get("authentication.login").tag("result", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordLogin_shouldIncrementCounterWithFailureTag_whenLoginFails() {
        authenticationMetrics.recordLogin(false);

        assertThat(registry.get("authentication.login").tag("result", "failure").counter().count()).isEqualTo(1.0);
    }
}
