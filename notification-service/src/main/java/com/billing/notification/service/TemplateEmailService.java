package com.billing.notification.service;

import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.PlanResponseDTO;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.SubscriptionEventNotification;
import com.billing.notification.model.SubscriptionNotificationEvent;
import com.billing.notification.utils.TemplateNotificationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.model.*;

@Component
@RequiredArgsConstructor
public class TemplateEmailService {

    private final TemplateNotificationUtils utils;

    public SendEmailRequest handlerNotificationTemplate(Notification notification){
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

        String processedTemplate = utils.buildEmailTemplate(eventNotification);
        return utils.buildEmailRequest(notification, processedTemplate);
    }

    private SubscriptionNotificationEvent extractEvent(Notification notification){
        Object body = notification.getBody();
        if(body instanceof SubscriptionCreatedEvent event) return event;
        if(body instanceof SubscriptionPaymentEvent event) return event;
        throw new IllegalArgumentException("Unsupported notification body: " + body.getClass().getSimpleName());
    }
}
