package com.billing.notification.service;

import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.PlanResponseDTO;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.mapper.NotificationMapper;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import com.billing.notification.model.SubscriptionEventNotification;
import com.billing.notification.utils.TemplateNotificationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.model.*;

@Component
@RequiredArgsConstructor
public class TemplateEmailService {

    private final TemplateNotificationUtils utils;
    private final NotificationMapper mapper;

    public SendEmailRequest handlerNotificationTemplate(Notification notification){
        return switch (notification.getTemplate()) {
            case SUBSCRIPTION_CREATED -> buildEmailByNewSubscriptionEvent(notification);
            case SUBSCRIPTION_PAID -> buildEmailByPaidSubscriptionEvent(notification);
            default -> throw new IllegalArgumentException("Template not supported: " + notification.getTemplate());
        };
    }

    private SendEmailRequest buildEmailByPaidSubscriptionEvent(Notification notification) {
        SubscriptionPaymentEvent event = (SubscriptionPaymentEvent) notification.getBody();

        String template = utils.loadTemplate(notification.getTemplate());
        CustomerClientResponse customer = event.customer();
        PlanResponseDTO plan = event.plan();

        SubscriptionEventNotification eventNotification = SubscriptionEventNotification.builder()
                .subscriptionId(event.subscriptionId())
                .subscriptionStatus(event.subscriptionStatus())
                .customer(customer)
                .plan(plan)
                .template(template)
                .currentPeriodStart(event.currentPeriodStart())
                .currentPeriodEnd(event.currentPeriodEnd())
                .build();

        String processedTemplate = utils.buildEmailTemplate(eventNotification);
        return utils.buildEmailRequest(notification, processedTemplate);
    }

    private SendEmailRequest buildEmailByNewSubscriptionEvent(Notification notification) {
        SubscriptionCreatedEvent event = (SubscriptionCreatedEvent) notification.getBody();

        String template = utils.loadTemplate(notification.getTemplate());
        CustomerClientResponse customer = event.customer();
        PlanResponseDTO plan = event.plan();

        SubscriptionEventNotification eventNotification = SubscriptionEventNotification.builder()
                .subscriptionId(event.id())
                .subscriptionStatus(event.subscriptionStatus())
                .customer(customer)
                .plan(plan)
                .template(template)
                .currentPeriodStart(event.currentPeriodStart())
                .currentPeriodEnd(event.currentPeriodEnd())
                .build();

        String processedTemplate = utils.buildEmailTemplate(eventNotification);
        return utils.buildEmailRequest(notification, processedTemplate);
    }
}
