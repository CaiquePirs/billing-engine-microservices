package com.billing.notification.service;

import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.SubscriptionEventNotification;
import com.billing.notification.model.SubscriptionNotificationEvent;
import com.billing.notification.utils.TemplateNotificationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Component
@RequiredArgsConstructor
public class TemplateEmailService {

    private final TemplateNotificationUtils utils;

    public SendEmailRequest handlerNotificationTemplate(Notification notification) {
        String processedTemplate = buildHtmlBody(notification);
        return utils.buildEmailRequest(notification, processedTemplate);
    }

    public String buildHtmlBody(Notification notification) {
        SubscriptionNotificationEvent event = extractEvent(notification);
        String template = utils.loadTemplate(notification.getTemplate());

        SubscriptionEventNotification eventNotification = SubscriptionEventNotification.builder()
                .subscriptionId(event.subscriptionId())
                .subscriptionStatus(event.subscriptionStatus())
                .customer(event.customer())
                .plan(event.plan())
                .template(template)
                .currentPeriodStart(event.currentPeriodStart())
                .currentPeriodEnd(event.currentPeriodEnd())
                .build();

        return utils.buildEmailTemplate(eventNotification);
    }

    private SubscriptionNotificationEvent extractEvent(Notification notification) {
        Object body = notification.getBody();
        if (body instanceof SubscriptionCreatedEvent event) return event;
        if (body instanceof SubscriptionPaymentEvent event) return event;
        if (body instanceof InvoiceCreatedEvent event) return event;
        throw new IllegalArgumentException("Unsupported notification body: " + body.getClass().getSimpleName());
    }
}
