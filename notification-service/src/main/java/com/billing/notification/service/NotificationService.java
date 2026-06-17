package com.billing.notification.service;

import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.mapper.NotificationMapper;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import com.billing.notification.strategy.port.SendNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SendNotificationPort sendNotificationPort;
    private final NotificationMapper notificationMapper;

    public void sendNewSubscriptionNotification(SubscriptionCreatedEvent event) {
        Notification notification = notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_CREATED);
        sendNotificationPort.sendEmail(notification);
    }

    public void sendPaymentApprovedNotification(SubscriptionPaymentEvent event) {
        Notification notification = notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_PAID);
        sendNotificationPort.sendEmail(notification);
    }

    public void sendPaymentFailedNotification(SubscriptionPaymentEvent event) {
        Notification notification = notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_CANCELLED);
        sendNotificationPort.sendEmail(notification);
    }
}
