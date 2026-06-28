package com.billing.notification.events.consumer;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.SnsMessage;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @SqsListener("${aws.sqs.queue.notify-subscription}")
    public void processNewSubscriptionEvent(SnsMessage snsMessage) {
        try {
            SubscriptionCreatedEvent event = objectMapper.readValue(
                    snsMessage.Message(),
                    SubscriptionCreatedEvent.class);

            notificationService.sendNewSubscriptionNotification(event);

        } catch (Exception e) {
            log.error("Failed to process new subscription notification event from SQS. Raw message: {}", snsMessage.Message(), e);
            throw new InternalNotificationErrorException("Failed to send new subscription notification email. Check event payload and SES configuration.", e);
        }
    }

    @SqsListener("${aws.sqs.queue.notify-payment-approved}")
    public void processPaymentApprovedEvent(SnsMessage snsMessage) {
        try {
            SubscriptionPaymentEvent event = objectMapper.readValue(
                    snsMessage.Message(),
                    SubscriptionPaymentEvent.class);

            notificationService.sendPaymentApprovedNotification(event);

        } catch (Exception e) {
            log.error("Failed to process payment-approved notification event from SQS. Raw message: {}", snsMessage.Message(), e);
            throw new InternalNotificationErrorException("Failed to send payment-approved notification email.", e);
        }
    }

    @SqsListener("${aws.sqs.queue.notify-payment-failed}")
    public void processPaymentFailedEvent(SnsMessage snsMessage) {
        try {
            SubscriptionPaymentEvent event = objectMapper.readValue(
                    snsMessage.Message(),
                    SubscriptionPaymentEvent.class);

            notificationService.sendPaymentFailedNotification(event);

        } catch (Exception e) {
            log.error("Failed to process payment-failed notification event from SQS. Raw message: {}", snsMessage.Message(), e);
            throw new InternalNotificationErrorException("Failed to send payment-failed notification email.", e);
        }
    }

    @SqsListener("${aws.sqs.queue.invoice-created}")
    public void processInvoiceCreatedEvent(String rawMessage) {
        try {
            InvoiceCreatedEvent event = objectMapper.readValue(
                            rawMessage,
                            InvoiceCreatedEvent.class);

            notificationService.sendInvoiceCreatedNotification(event);

        } catch (Exception e) {
            log.error("Failed to process invoice-created notification event from SQS. Raw message: {}", rawMessage, e);
            throw new InternalNotificationErrorException("Failed to send invoice-created notification email.", e);
        }
    }
}
