package com.billing.invoice.events.tracing;

import com.billing.invoice.events.data.SnsMessage;
import com.billing.invoice.events.data.SnsMessageAttribute;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingTracingTest {

    @Mock private Tracer tracer;
    @Mock private Propagator propagator;
    @Mock private CurrentTraceContext currentTraceContext;
    @Mock private Span span;
    @Mock private Span.Builder spanBuilder;

    @InjectMocks
    private MessagingTracing messagingTracing;

    @BeforeEach
    void setUp() {
        lenient().when(tracer.currentTraceContext()).thenReturn(currentTraceContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    void currentTraceHeaders_shouldInjectContext_whenActiveSpanExists() {
        TraceContext context = mock(TraceContext.class);
        when(currentTraceContext.context()).thenReturn(context);
        doAnswer(invocation -> {
            Map<String, String> carrier = invocation.getArgument(1);
            Propagator.Setter<Map<String, String>> setter = invocation.getArgument(2);
            setter.set(carrier, "traceparent", "00-trace-span-01");
            return null;
        }).when(propagator).inject(eq(context), any(), any());

        Map<String, String> headers = messagingTracing.currentTraceHeaders();

        assertThat(headers).containsEntry("traceparent", "00-trace-span-01");
    }

    @Test
    void currentTraceHeaders_shouldReturnEmpty_whenNoActiveContext() {
        when(currentTraceContext.context()).thenReturn(null);

        Map<String, String> headers = messagingTracing.currentTraceHeaders();

        assertThat(headers).isEmpty();
        verify(propagator, never()).inject(any(), any(), any());
    }

    @Test
    void traceConsume_shouldRunActionInsideSpanAndEndIt_whenActionSucceeds() {
        stubExtractedSpan();
        SnsMessage message = new SnsMessage("Notification", "{}",
                Map.of("traceparent", new SnsMessageAttribute("String", "00-trace-span-01")));
        AtomicBoolean ran = new AtomicBoolean(false);

        messagingTracing.traceConsume("test process", message, () -> ran.set(true));

        assertThat(ran).isTrue();
        verify(span).end();
        verify(span, never()).error(any());
    }

    @Test
    void traceConsume_shouldMarkSpanErrorAndRethrow_whenActionThrows() {
        stubExtractedSpan();
        SnsMessage message = new SnsMessage("Notification", "{}", null);
        RuntimeException failure = new RuntimeException("boom");

        assertThatThrownBy(() -> messagingTracing.traceConsume("test process", message, () -> {
            throw failure;
        })).isSameAs(failure);

        verify(span).error(failure);
        verify(span).end();
    }

    @SuppressWarnings("unchecked")
    private void stubExtractedSpan() {
        when(propagator.extract(any(), any())).thenReturn(spanBuilder);
        when(spanBuilder.kind(any())).thenReturn(spanBuilder);
        when(spanBuilder.name(any())).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(span);
        when(tracer.withSpan(span)).thenReturn(mock(Tracer.SpanInScope.class));
    }
}
