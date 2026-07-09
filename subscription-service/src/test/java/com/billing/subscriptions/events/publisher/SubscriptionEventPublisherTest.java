package com.billing.subscriptions.events.publisher;

import com.billing.subscriptions.controller.advice.exception.ExternalServiceException;
import com.billing.subscriptions.events.data.SubscriptionCreatedEvent;
import com.billing.subscriptions.events.tracing.MessagingTracing;
import com.billing.subscriptions.metrics.BillingSubscriptionMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionEventPublisherTest {

    @Mock private SnsClient snsClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private BillingSubscriptionMetrics billingSubscriptionMetrics;
    @Mock private MessagingTracing messagingTracing;

    @InjectMocks
    private SubscriptionEventPublisher subscriptionEventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionEventPublisher, "snsTopicSubscriptionCreated",
                "arn:aws:sns:us-east-1:000000000000:subscription-created-topic");
    }

    private SubscriptionCreatedEvent buildEvent() {
        return SubscriptionCreatedEvent.builder()
                .subscriptionId(UUID.randomUUID())
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .subscriptionStatus("PENDING")
                .paymentMethodId("pm_test123")
                .build();
    }

    @Test
    void publisherSubscriptionCreated_shouldPublishEvent_whenSerializationSucceeds() throws Exception {
        SubscriptionCreatedEvent event = buildEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"subscriptionId\":\"some-id\"}");
        when(messagingTracing.currentTraceHeaders()).thenReturn(Map.of("traceparent", "00-abc-def-01"));

        subscriptionEventPublisher.publisherSubscriptionCreated(event);

        verify(snsClient).publish(any(PublishRequest.class));
    }

    @Test
    void publisherSubscriptionCreated_shouldThrowExternalServiceException_whenSerializationFails() throws Exception {
        SubscriptionCreatedEvent event = buildEvent();
        when(objectMapper.writeValueAsString(event)).thenThrow(new RuntimeException("Serialization error"));

        assertThatThrownBy(() -> subscriptionEventPublisher.publisherSubscriptionCreated(event))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Failed to publish subscription-created SNS event");

        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void publisherSubscriptionCreated_shouldThrowExternalServiceException_whenSnsClientFails() throws Exception {
        SubscriptionCreatedEvent event = buildEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"subscriptionId\":\"some-id\"}");
        when(messagingTracing.currentTraceHeaders()).thenReturn(Map.of());
        when(snsClient.publish(any(PublishRequest.class))).thenThrow(new RuntimeException("SNS unavailable"));

        assertThatThrownBy(() -> subscriptionEventPublisher.publisherSubscriptionCreated(event))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Failed to publish subscription-created SNS event");
    }
}
