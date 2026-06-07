package com.billing.notification.utils;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import com.billing.notification.events.data.CustomerAddressResponse;
import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.services.ses.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Component
public class TemplateNotificationUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final String NEW_SUBSCRIPTION_TEMPLATE = "templates/new-subscription-email.html";

    public String loadTemplate(NotificationTemplate template) {
        ClassPathResource resource;
        String templateContent = "";

        try {
            if (Objects.requireNonNull(template) == NotificationTemplate.SUBSCRIPTION_CREATED) {
                resource = new ClassPathResource(NEW_SUBSCRIPTION_TEMPLATE);
                templateContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }

        } catch (Exception e) {
            throw new InternalNotificationErrorException("Failed to load email template: " + NEW_SUBSCRIPTION_TEMPLATE, e);
        }

        return templateContent;
    }

    public SubscriptionCreatedEvent extractSubscriptionCreatedEvent(Notification notification) {
        Object event = notification.getBody().get(NotificationTemplate.SUBSCRIPTION_CREATED);

        if (!(event instanceof SubscriptionCreatedEvent subscriptionCreatedEvent)) {
            throw new InternalAuthenticationServiceException("Subscription created event not found in notification body");
        }

        return subscriptionCreatedEvent;
    }

    public SendEmailRequest buildEmailRequest(Notification notification, String template){
        return SendEmailRequest.builder()
                .source(notification.getFrom())
                .destination(Destination.builder().toAddresses(notification.getTo()).build())
                .message(Message.builder().subject(Content.builder()
                                .charset("UTF-8")
                                .data("New Subscription Created")
                                .build())
                        .body(Body.builder()
                                .html(Content.builder()
                                        .charset("UTF-8")
                                        .data(template)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public String formatMoney(BigDecimal price, String currency) {
        if (price == null || currency == null || currency.isBlank()) return "-";

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.ENGLISH);
        formatter.setCurrency(java.util.Currency.getInstance(currency));

        return formatter.format(price);
    }

    public String buildCustomerAddress(CustomerAddressResponse address) {
        if (address == null) return "-";
        return String.join(", ",
                safeValue(address.street()),
                safeValue(address.number()),
                safeValue(address.city()),
                safeValue(address.state()),
                safeValue(address.county()),
                safeValue(address.eircode())
        );
    }

    public String buildCustomerFullName(CustomerClientResponse customer) {
        return safeValue(customer.name()) + " " + safeValue(customer.lastName());
    }

    public String formatDate(java.time.LocalDate date) {
        if (date == null) return "-";
        return date.format(DATE_FORMATTER);
    }

    public String safeValue(Object value) {
        if (value == null) return "-";
        String text = value.toString();
        if (text.isBlank()) return "-";
        return text;
    }

}
