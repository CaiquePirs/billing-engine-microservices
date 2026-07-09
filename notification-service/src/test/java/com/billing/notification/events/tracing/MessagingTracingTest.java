package com.billing.notification.events.tracing;

import com.billing.notification.events.data.SnsMessage;
import com.billing.notification.events.data.SnsMessageAttribute;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingTracingTest {

    @Mock private Tracer tracer;
    @Mock private Propagator propagator;
    @Mock private Span span;
    @Mock private Span.Builder spanBuilder;

    @InjectMocks
    private MessagingTracing messagingTracing;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(propagator.extract(any(), any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.kind(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.name(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.start()).thenReturn(span);
        lenient().when(tracer.withSpan(span)).thenReturn(mock(Tracer.SpanInScope.class));
    }

    @Test
    void traceConsume_shouldRunActionFromSnsEnvelopeAndEndSpan_whenActionSucceeds() {
        SnsMessage message = new SnsMessage("Notification", "{}",
                Map.of("traceparent", new SnsMessageAttribute("String", "00-trace-span-01")));
        AtomicBoolean ran = new AtomicBoolean(false);

        messagingTracing.traceConsume("test process", message, () -> ran.set(true));

        assertThat(ran).isTrue();
        verify(span).end();
        verify(span, never()).error(any());
    }

    @Test
    void traceConsume_shouldRunActionFromRawHeadersAndEndSpan_whenActionSucceeds() {
        AtomicBoolean ran = new AtomicBoolean(false);

        messagingTracing.traceConsume("test process", Map.of("traceparent", "00-trace-span-01"), () -> ran.set(true));

        assertThat(ran).isTrue();
        verify(span).end();
    }

    @Test
    void traceConsume_shouldHandleNullHeaders_whenNoTraceparentPresent() {
        AtomicBoolean ran = new AtomicBoolean(false);

        messagingTracing.traceConsume("test process", (Map<String, String>) null, () -> ran.set(true));

        assertThat(ran).isTrue();
        verify(span).end();
    }

    @Test
    void traceConsume_shouldMarkSpanErrorAndRethrow_whenActionThrows() {
        SnsMessage message = new SnsMessage("Notification", "{}", null);
        RuntimeException failure = new RuntimeException("boom");

        assertThatThrownBy(() -> messagingTracing.traceConsume("test process", message, () -> {
            throw failure;
        })).isSameAs(failure);

        verify(span).error(failure);
        verify(span).end();
    }
}
