package com.billing.invoice.events.tracing;

import com.billing.invoice.events.data.SnsMessage;
import com.billing.invoice.events.data.SnsMessageAttribute;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Carries the distributed-trace context across the async SNS/SQS boundary.
 *
 * <p>Producers call {@link #currentTraceHeaders()} to serialize the active trace
 * context (W3C {@code traceparent}) into message attributes. Consumers call
 * {@link #traceConsume} to re-open that context so their processing (and any
 * downstream DB/HTTP/SQS spans) joins the same trace instead of starting a new one.
 */
@Component
@RequiredArgsConstructor
public class MessagingTracing {

    private final Tracer tracer;
    private final Propagator propagator;

    /** Producer side: current trace context as propagation headers (empty when no active span). */
    public Map<String, String> currentTraceHeaders() {
        Map<String, String> headers = new HashMap<>();
        TraceContext context = tracer.currentTraceContext().context();
        if (context != null) {
            propagator.inject(context, headers, Map::put);
        }
        return headers;
    }

    /** Consumer side: run {@code action} inside a CONSUMER span continuing the upstream trace. */
    public void traceConsume(String spanName, SnsMessage snsMessage, Runnable action) {
        Map<String, String> headers = toHeaders(snsMessage.MessageAttributes());
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
