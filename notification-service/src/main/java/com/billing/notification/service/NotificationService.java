package com.billing.notification.service;

import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import com.billing.notification.strategy.port.SendNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SendNotificationPort sendNotificationPort;

    @Value("${INTERNAL_SERVICE_EMAIL}")
    private String internalEmail;

    public void sendNewSubscriptionNotification(SubscriptionCreatedEvent event) {
        Notification notification = Notification.builder()
                .to(event.customer().email())
                .from(internalEmail)
                .body(event)
                .template(NotificationTemplate.SUBSCRIPTION_CREATED)
                .build();

        sendNotificationPort.sendEmail(notification);
    }

    public void sendPaymentApprovedNotification(SubscriptionPaymentEvent event) {
        Notification notification = Notification.builder()
                .to(event.customer().email())
                .from(internalEmail)
                .body(event)
                .template(NotificationTemplate.SUBSCRIPTION_PAID)
                .build();

        sendNotificationPort.sendEmail(notification);
    }
}
