package com.billing.notification.service;

import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import com.billing.notification.strategy.port.SendNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
                .body(Map.of(NotificationTemplate.SUBSCRIPTION_CREATED, event))
                .build();

        sendNotificationPort.sendEmail(notification);
    }
}
