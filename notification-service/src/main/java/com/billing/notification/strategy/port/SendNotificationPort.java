package com.billing.notification.strategy.port;

import com.billing.notification.model.Notification;

public interface SendNotificationPort {
    void sendEmail(Notification notification);
}
