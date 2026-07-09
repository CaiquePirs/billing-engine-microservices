package com.billing.payment.events.publisher;

import com.billing.payment.controller.advice.exceptions.InternalErrorException;
import com.billing.payment.events.data.CustomerClientResponse;
import com.billing.payment.events.data.PlanResponseDTO;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.data.SubscriptionPaymentEvent;
import com.billing.payment.events.tracing.MessagingTracing;
import com.billing.payment.metrics.PaymentMetrics;
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
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock private SqsClient sqsClient;
    @Mock private SnsClient snsClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private PaymentMetrics paymentMetrics;
    @Mock private MessagingTracing messagingTracing;

    @InjectMocks
    private PaymentEventPublisher paymentEventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentEventPublisher, "dlqQueue", "https://sqs.us-east-1.amazonaws.com/000000000000/dlq");
        ReflectionTestUtils.setField(paymentEventPublisher, "snsTopicPaymentApproved", "arn:aws:sns:us-east-1:000000000000:payment-approved");
        ReflectionTestUtils.setField(paymentEventPublisher, "snsTopicPaymentFailed", "arn:aws:sns:us-east-1:000000000000:payment-failed");
        lenient().when(messagingTracing.currentTraceHeaders()).thenReturn(Map.of("traceparent", "00-abc-def-01"));
    }

    private SubscriptionPaymentEvent buildPaymentEvent() {
        CustomerClientResponse customer = CustomerClientResponse.builder()
                .id(UUID.randomUUID()).email("john@example.com").build();
        PlanResponseDTO plan = PlanResponseDTO.builder()
                .price(new BigDecimal("9900")).currency("EUR").build();
        return SubscriptionPaymentEvent.builder()
                .paymentId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .paymentStatus("APPROVED")
                .subscriptionStatus("ACTIVE")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(customer)
                .plan(plan)
                .build();
    }

    private SubscriptionCreatedEvent buildCreatedEvent() {
        CustomerClientResponse customer = CustomerClientResponse.builder()
                .id(UUID.randomUUID()).email("john@example.com").build();
        PlanResponseDTO plan = PlanResponseDTO.builder()
                .price(new BigDecimal("9900")).currency("EUR").build();
        return new SubscriptionCreatedEvent(UUID.randomUUID(), LocalDate.now(),
                LocalDate.now().plusMonths(1), "PENDING", "pm_test", customer, plan);
    }

    @Test
    void publisherPaymentApproved_shouldPublishToSns_whenSerializationSucceeds() throws Exception {
        SubscriptionPaymentEvent event = buildPaymentEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"paymentId\":\"some-id\"}");

        paymentEventPublisher.publisherPaymentApproved(event);

        verify(snsClient).publish(any(PublishRequest.class));
    }

    @Test
    void publisherPaymentApproved_shouldThrowInternalErrorException_whenSnsClientFails() throws Exception {
        SubscriptionPaymentEvent event = buildPaymentEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"paymentId\":\"some-id\"}");
        when(snsClient.publish(any(PublishRequest.class))).thenThrow(new RuntimeException("SNS error"));

        assertThatThrownBy(() -> paymentEventPublisher.publisherPaymentApproved(event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Failed to publish payment-approved SNS event");
    }

    @Test
    void publisherPaymentFailed_shouldPublishToSns_whenSerializationSucceeds() throws Exception {
        SubscriptionPaymentEvent event = buildPaymentEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"paymentId\":\"some-id\"}");

        paymentEventPublisher.publisherPaymentFailed(event);

        verify(snsClient).publish(any(PublishRequest.class));
    }

    @Test
    void publisherPaymentFailed_shouldThrowInternalErrorException_whenSnsClientFails() throws Exception {
        SubscriptionPaymentEvent event = buildPaymentEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"paymentId\":\"some-id\"}");
        when(snsClient.publish(any(PublishRequest.class))).thenThrow(new RuntimeException("SNS error"));

        assertThatThrownBy(() -> paymentEventPublisher.publisherPaymentFailed(event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Failed to publish payment-failed SNS event");
    }

    @Test
    void sendPaymentToDlqQueue_shouldSendToSqs_whenSerializationSucceeds() throws Exception {
        SubscriptionCreatedEvent event = buildCreatedEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"subscriptionId\":\"some-id\"}");

        paymentEventPublisher.sendPaymentToDlqQueue(event);

        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void sendPaymentToDlqQueue_shouldThrowInternalErrorException_whenSqsClientFails() throws Exception {
        SubscriptionCreatedEvent event = buildCreatedEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"subscriptionId\":\"some-id\"}");
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(new RuntimeException("SQS error"));

        assertThatThrownBy(() -> paymentEventPublisher.sendPaymentToDlqQueue(event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Failed to forward subscription ID");
    }
}
