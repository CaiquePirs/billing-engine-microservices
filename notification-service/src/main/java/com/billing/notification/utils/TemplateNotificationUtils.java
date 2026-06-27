package com.billing.notification.utils;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import com.billing.notification.events.data.CustomerAddressResponse;
import com.billing.notification.model.SubscriptionEventNotification;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.services.ses.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

@Component
public class TemplateNotificationUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final String NEW_SUBSCRIPTION_TEMPLATE = "templates/new-subscription-email.html";
    private static final String PAID_SUBSCRIPTION_TEMPLATE = "templates/paid-subscription-email.html";
    private static final String FAILED_SUBSCRIPTION_TEMPLATE = "templates/failed-subscription-email.html";
    private static final String INVOICE_CREATED_TEMPLATE = "templates/invoice-created-subscription-email.html";

    public String loadTemplate(NotificationTemplate template) {
        ClassPathResource resource;
        String templateContent = "";

        try {
            if (Objects.requireNonNull(template) == NotificationTemplate.SUBSCRIPTION_CREATED) {
                resource = new ClassPathResource(NEW_SUBSCRIPTION_TEMPLATE);
                templateContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }

            if (Objects.requireNonNull(template) == NotificationTemplate.SUBSCRIPTION_PAID) {
                resource = new ClassPathResource(PAID_SUBSCRIPTION_TEMPLATE);
                templateContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }

            if (Objects.requireNonNull(template) == NotificationTemplate.SUBSCRIPTION_CANCELLED) {
                resource = new ClassPathResource(FAILED_SUBSCRIPTION_TEMPLATE);
                templateContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }

            if (Objects.requireNonNull(template) == NotificationTemplate.INVOICE_CREATED) {
                resource = new ClassPathResource(INVOICE_CREATED_TEMPLATE);
                templateContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }


        } catch (Exception e) {
            throw new InternalNotificationErrorException("Failed to load email template: " + template, e);
        }

        return templateContent;
    }

    public String buildEmailTemplate(SubscriptionEventNotification eventNotification){
        return eventNotification.template()
                .replace("{{subscriptionId}}", safeValue(eventNotification.subscriptionId()))
                .replace("{{subscriptionStatus}}", safeValue(eventNotification.subscriptionStatus()))
                .replace("{{currentPeriodStart}}", formatDate(eventNotification.currentPeriodStart()))
                .replace("{{currentPeriodEnd}}", formatDate(eventNotification.currentPeriodEnd()))
                .replace("{{planName}}", safeValue(eventNotification.plan().name()))
                .replace("{{planInterval}}", safeValue(eventNotification.plan().interval()))
                .replace("{{planPrice}}", formatMoney(eventNotification.plan().price(), eventNotification.plan().currency()))
                .replace("{{customerFullName}}", eventNotification.customer().name())
                .replace("{{customerEmail}}", safeValue(eventNotification.customer().email()))
                .replace("{{customerPhone}}", safeValue(eventNotification.customer().phone()))
                .replace("{{customerAddress}}", buildEmailCustomerAddress(eventNotification.customer().address()));
    }

    public SendEmailRequest buildEmailRequest(Notification notification, String template){
        return SendEmailRequest.builder()
                .source(notification.getFrom())
                .destination(Destination.builder().toAddresses(notification.getTo()).build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .charset("UTF-8")
                                .data(buildEmailTitle(notification.getTemplate()))
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

    private String buildEmailCustomerAddress(CustomerAddressResponse address) {
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

    public String buildEmailTitle(NotificationTemplate template){
        return switch (template) {
            case SUBSCRIPTION_CREATED -> "Subscription confirmation";
            case SUBSCRIPTION_PAID -> "Subscription Paid";
            case SUBSCRIPTION_CANCELLED -> "Subscription Cancelled";
            case INVOICE_CREATED -> "Your Invoice is Ready";
            default -> throw new InternalNotificationErrorException("Template not supported: " + template);
        };
    }

    private String formatMoney(BigDecimal price, String currency) {
        if (price == null || currency == null || currency.isBlank()) {
            return "-";
        }

        try {
            Currency validCurrency = Currency.getInstance(currency.trim().toUpperCase(Locale.ROOT));
            BigDecimal normalizedPrice = price.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IE"));
            formatter.setCurrency(validCurrency);

            return formatter.format(normalizedPrice);

        } catch (IllegalArgumentException e) {
            throw new InternalNotificationErrorException("Invalid currency code for email template: " + currency, e);
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "-";
        return date.format(DATE_FORMATTER);
    }

    private String safeValue(Object value) {
        if (value == null) return "-";
        String text = value.toString();
        if (text.isBlank()) return "-";
        return text;
    }

}
