package com.billing.notification.strategy.adapter;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import com.billing.notification.model.Notification;
import com.billing.notification.service.TemplateEmailService;
import com.billing.notification.strategy.port.SendNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendNotificationAdapter implements SendNotificationPort {

    private final SesClient sesClient;
    private final TemplateEmailService templateEmailService;

    @Override
    public void sendEmail(Notification notification) {
        try {
            SendEmailRequest newSubscriptionEmail = templateEmailService.handlerNotificationTemplate(notification);
            sesClient.sendEmail(newSubscriptionEmail);

        } catch (Exception e) {
            log.error("Error sending email: {}", notification, e);
            throw new InternalNotificationErrorException("Failed to send email", e);
        }
    }
}