package com.billing.notification.service;

import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.PlanResponseDTO;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import com.billing.notification.utils.TemplateNotificationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.model.*;

@Component
@RequiredArgsConstructor
public class TemplateEmailService {

    private final TemplateNotificationUtils utils;

    public SendEmailRequest buildNewSubscriptionEmail(Notification notification) {
        SubscriptionCreatedEvent event = utils.extractSubscriptionCreatedEvent(notification);

        String template = utils.loadTemplate(NotificationTemplate.SUBSCRIPTION_CREATED);
        CustomerClientResponse customer = event.customer();
        PlanResponseDTO plan = event.plan();

        String processedTemplate = template
                .replace("{{subscriptionId}}", utils.safeValue(event.id()))
                .replace("{{subscriptionStatus}}", utils.safeValue(event.subscriptionStatus()))
                .replace("{{currentPeriodStart}}", utils.formatDate(event.currentPeriodStart()))
                .replace("{{currentPeriodEnd}}", utils.formatDate(event.currentPeriodEnd()))
                .replace("{{planName}}", utils.safeValue(plan.name()))
                .replace("{{planDescription}}", utils.safeValue(plan.description()))
                .replace("{{planInterval}}", utils.safeValue(plan.interval()))
                .replace("{{planPrice}}", utils.formatMoney(plan.price(), plan.currency()))
                .replace("{{customerFullName}}", utils.buildCustomerFullName(customer))
                .replace("{{customerEmail}}", utils.safeValue(customer.email()))
                .replace("{{customerPhone}}", utils.safeValue(customer.phone()))
                .replace("{{customerTaxNumber}}", utils.safeValue(customer.taxNumber()))
                .replace("{{customerAge}}", utils.safeValue(customer.age()))
                .replace("{{customerStatus}}", utils.safeValue(customer.customerStatus()))
                .replace("{{customerAddress}}", utils.buildCustomerAddress(customer.address()));

        return utils.buildEmailRequest(notification, processedTemplate);
    }
}
