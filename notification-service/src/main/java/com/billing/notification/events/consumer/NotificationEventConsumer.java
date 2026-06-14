package com.billing.notification.events.consumer;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
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

    @SqsListener("${NOTIFY_SUBSCRIPTION_QUEUE}")
    public void processNewSubscriptionEvent(SnsMessage snsMessage) {
        try {
            SubscriptionCreatedEvent event = objectMapper.readValue(
                    snsMessage.Message(),
                    SubscriptionCreatedEvent.class);

            notificationService.sendNewSubscriptionNotification(event);

        } catch (Exception e) {
            log.error("Error processing notification event: {}", snsMessage.Message(), e);
            throw new InternalNotificationErrorException("Failed to process subscription notification event", e);
        }
    }

    @SqsListener("${NOTIFY_PAYMENT_APPROVED}")
    public void processPaymentApprovedEvent(SnsMessage snsMessage) {
        try {
            SubscriptionPaymentEvent event = objectMapper.readValue(
                    snsMessage.Message(),
                    SubscriptionPaymentEvent.class);

           notificationService.sendPaymentApprovedNotification(event);

        } catch (Exception e) {
            log.error("Error processing notification event: {}", snsMessage.Message(), e);
            throw new InternalNotificationErrorException("Failed to process subscription notification event", e);
        }
    }
}
