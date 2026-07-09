package com.billing.notification.events.tracing;

import com.billing.notification.events.data.SnsMessage;
import com.billing.notification.events.data.SnsMessageAttribute;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Re-opens the distributed-trace context carried across the async SNS/SQS boundary,
 * so a consumer's processing (and its downstream spans) joins the upstream trace
 * instead of starting a new one.
 *
 * <p>Two entry points are offered: one for SNS-to-SQS envelopes (attributes live
 * inside the JSON body) and one for raw SQS messages (attributes arrive as message
 * headers).
 */
@Component
@RequiredArgsConstructor
public class MessagingTracing {

    private final Tracer tracer;
    private final Propagator propagator;

    /** Continue the upstream trace carried in an SNS-to-SQS envelope's message attributes. */
    public void traceConsume(String spanName, SnsMessage snsMessage, Runnable action) {
        runInSpan(spanName, toHeaders(snsMessage.MessageAttributes()), action);
    }

    /** Continue the upstream trace carried as raw SQS message headers (e.g. {@code traceparent}). */
    public void traceConsume(String spanName, Map<String, String> headers, Runnable action) {
        runInSpan(spanName, headers == null ? new HashMap<>() : headers, action);
    }

    private void runInSpan(String spanName, Map<String, String> headers, Runnable action) {
        Span span = propagator.extract(headers, Map::get)
                .kind(Span.Kind.CONSUMER)
                .name(spanName)
                .start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            action.run();
        } catch (RuntimeException e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private Map<String, String> toHeaders(Map<String, SnsMessageAttribute> attributes) {
        Map<String, String> headers = new HashMap<>();
        if (attributes != null) {
            attributes.forEach((key, attribute) -> {
                if (attribute != null && attribute.Value() != null) {
                    headers.put(key, attribute.Value());
                }
            });
        }
        return headers;
    }
}
