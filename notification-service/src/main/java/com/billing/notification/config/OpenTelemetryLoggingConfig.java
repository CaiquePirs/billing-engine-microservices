package com.billing.notification.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.context.annotation.Configuration;

/**
 * Connects the auto-configured OpenTelemetry SDK to the Logback OpenTelemetryAppender
 * registered in logback-spring.xml, so application logs are exported over OTLP.
 */
@Configuration
public class OpenTelemetryLoggingConfig {

    public OpenTelemetryLoggingConfig(OpenTelemetry openTelemetry) {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
