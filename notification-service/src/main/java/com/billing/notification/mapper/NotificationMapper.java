package com.billing.notification.mapper;

import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    @Value("${INTERNAL_SERVICE_EMAIL}")
    private String internalEmail;

    public Notification mapToNotification(Object event, NotificationTemplate template){
        if (template.equals(NotificationTemplate.SUBSCRIPTION_CREATED)) {
            SubscriptionCreatedEvent subscriptionCreatedEvent = (SubscriptionCreatedEvent) event;
            return Notification.builder()
                    .to(subscriptionCreatedEvent.customer().email())
                    .from(internalEmail)
                    .body(event)
                    .template(NotificationTemplate.SUBSCRIPTION_CREATED)
                    .build();
        }

        if (template.equals(NotificationTemplate.INVOICE_CREATED)) {
            InvoiceCreatedEvent invoiceCreatedEvent = (InvoiceCreatedEvent) event;
            return Notification.builder()
                    .to(invoiceCreatedEvent.customer().email())
                    .from(internalEmail)
                    .body(event)
                    .template(NotificationTemplate.INVOICE_CREATED)
                    .build();
        }

        SubscriptionPaymentEvent subscriptionPaymentEvent = (SubscriptionPaymentEvent) event;
        return Notification.builder()
                .to(subscriptionPaymentEvent.customer().email())
                .from(internalEmail)
                .body(subscriptionPaymentEvent)
                .template(template)
                .build();
    }
}
