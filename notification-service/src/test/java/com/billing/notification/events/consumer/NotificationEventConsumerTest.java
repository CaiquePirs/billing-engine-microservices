package com.billing.notification.events.consumer;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.SnsMessage;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SnsMessage;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.events.tracing.MessagingTracing;
import com.billing.notification.metrics.NotificationMetrics;
import com.billing.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock private NotificationService notificationService;
    @Mock private ObjectMapper objectMapper;
    @Mock private NotificationMetrics notificationMetrics;
    @Mock private MessagingTracing messagingTracing;

    @InjectMocks
    private NotificationEventConsumer notificationEventConsumer;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return null;
        }).when(messagingTracing).traceConsume(anyString(), any(SnsMessage.class), any(Runnable.class));

        lenient().doAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return null;
        }).when(messagingTracing).traceConsume(anyString(), anyMap(), any(Runnable.class));
    }

    @Test
    void processNewSubscriptionEvent_shouldDeserializeAndSendNotification_whenMessageIsValid() throws Exception {
        String messageBody = "{\"subscriptionId\":\"some-id\"}";
        SnsMessage snsMessage = new SnsMessage("Notification", messageBody);
        SubscriptionCreatedEvent event = mock(SubscriptionCreatedEvent.class);

        when(objectMapper.readValue(messageBody, SubscriptionCreatedEvent.class)).thenReturn(event);

        notificationEventConsumer.processNewSubscriptionEvent(snsMessage);

        verify(notificationService).sendNewSubscriptionNotification(event);
        verify(notificationMetrics).recordNewSubscriptionQueueMessageConsumedTotal();
    }

    @Test
    void processNewSubscriptionEvent_shouldThrowInternalNotificationErrorException_whenDeserializationFails() throws Exception {
        String messageBody = "invalid-json";
        SnsMessage snsMessage = new SnsMessage("Notification", messageBody);

        when(objectMapper.readValue(messageBody, SubscriptionCreatedEvent.class))
                .thenThrow(new RuntimeException("Deserialization error"));

        assertThatThrownBy(() -> notificationEventConsumer.processNewSubscriptionEvent(snsMessage))
                .isInstanceOf(InternalNotificationErrorException.class)
                .hasMessageContaining("Failed to send new subscription notification email");

        verify(notificationService, never()).sendNewSubscriptionNotification(any());
        verify(notificationMetrics).recordNewSubscriptionQueueMessageConsumptionFailedTotal();
    }

    @Test
    void processPaymentApprovedEvent_shouldDeserializeAndSendNotification_whenMessageIsValid() throws Exception {
        String messageBody = "{\"paymentId\":\"some-id\"}";
        SnsMessage snsMessage = new SnsMessage("Notification", messageBody);
        SubscriptionPaymentEvent event = mock(SubscriptionPaymentEvent.class);

        when(objectMapper.readValue(messageBody, SubscriptionPaymentEvent.class)).thenReturn(event);

        notificationEventConsumer.processPaymentApprovedEvent(snsMessage);

        verify(notificationService).sendPaymentApprovedNotification(event);
        verify(notificationMetrics).recordPaymentApprovedQueueMessageConsumedTotal();
    }

    @Test
    void processPaymentApprovedEvent_shouldThrowInternalNotificationErrorException_whenServiceFails() throws Exception {
        String messageBody = "{\"paymentId\":\"some-id\"}";
        SnsMessage snsMessage = new SnsMessage("Notification", messageBody);
        SubscriptionPaymentEvent event = mock(SubscriptionPaymentEvent.class);

        when(objectMapper.readValue(messageBody, SubscriptionPaymentEvent.class)).thenReturn(event);
        doThrow(new RuntimeException("SES error")).when(notificationService).sendPaymentApprovedNotification(event);

        assertThatThrownBy(() -> notificationEventConsumer.processPaymentApprovedEvent(snsMessage))
                .isInstanceOf(InternalNotificationErrorException.class)
                .hasMessageContaining("Failed to send payment-approved notification email");

        verify(notificationMetrics).recordPaymentApprovedQueueMessageConsumptionFailedTotal();
    }

    @Test
    void processPaymentFailedEvent_shouldDeserializeAndSendNotification_whenMessageIsValid() throws Exception {
        String messageBody = "{\"paymentId\":\"some-id\"}";
        SnsMessage snsMessage = new SnsMessage("Notification", messageBody);
        SubscriptionPaymentEvent event = mock(SubscriptionPaymentEvent.class);

        when(objectMapper.readValue(messageBody, SubscriptionPaymentEvent.class)).thenReturn(event);

        notificationEventConsumer.processPaymentFailedEvent(snsMessage);

        verify(notificationService).sendPaymentFailedNotification(event);
        verify(notificationMetrics).recordPaymentFailedQueueMessageConsumedTotal();
    }

    @Test
    void processPaymentFailedEvent_shouldThrowInternalNotificationErrorException_whenDeserializationFails() throws Exception {
        String messageBody = "bad-json";
        SnsMessage snsMessage = new SnsMessage("Notification", messageBody);

        when(objectMapper.readValue(messageBody, SubscriptionPaymentEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        assertThatThrownBy(() -> notificationEventConsumer.processPaymentFailedEvent(snsMessage))
                .isInstanceOf(InternalNotificationErrorException.class)
                .hasMessageContaining("Failed to send payment-failed notification email");

        verify(notificationService, never()).sendPaymentFailedNotification(any());
        verify(notificationMetrics).recordPaymentFailedQueueMessageConsumptionFailedTotal();
    }

    @Test
    void processInvoiceCreatedEvent_shouldDeserializeAndSendNotification_whenMessageIsValid() throws Exception {
        String rawMessage = "{\"invoiceId\":\"some-id\"}";
        InvoiceCreatedEvent event = mock(InvoiceCreatedEvent.class);

        when(objectMapper.readValue(rawMessage, InvoiceCreatedEvent.class)).thenReturn(event);

        notificationEventConsumer.processInvoiceCreatedEvent(rawMessage, "00-abc-def-01");

        verify(notificationService).sendInvoiceCreatedNotification(event);
        verify(notificationMetrics).recordInvoiceCreatedQueueMessageConsumedTotal();
    }

    @Test
    void processInvoiceCreatedEvent_shouldThrowInternalNotificationErrorException_whenDeserializationFails() throws Exception {
        String rawMessage = "not-valid-json";

        when(objectMapper.readValue(rawMessage, InvoiceCreatedEvent.class))
                .thenThrow(new RuntimeException("Deserialization error"));

        assertThatThrownBy(() -> notificationEventConsumer.processInvoiceCreatedEvent(rawMessage, null))
                .isInstanceOf(InternalNotificationErrorException.class)
                .hasMessageContaining("Failed to send invoice-created notification email");

        verify(notificationService, never()).sendInvoiceCreatedNotification(any());
        verify(notificationMetrics).recordInvoiceCreatedQueueMessageConsumptionFailedTotal();
    }
}
