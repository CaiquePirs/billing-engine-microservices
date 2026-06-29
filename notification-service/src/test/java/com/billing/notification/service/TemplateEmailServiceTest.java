package com.billing.notification.service;

import com.billing.notification.events.data.CustomerAddressResponse;
import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.PlanResponseDTO;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import com.billing.notification.utils.TemplateNotificationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateEmailServiceTest {

    @Mock private TemplateNotificationUtils utils;

    @InjectMocks
    private TemplateEmailService templateEmailService;

    private CustomerClientResponse buildCustomer() {
        CustomerAddressResponse address = new CustomerAddressResponse(
                UUID.randomUUID(), "Main St", "10", "Dublin", "Leinster", "Ireland", "D01 AB12");
        return new CustomerClientResponse(UUID.randomUUID(), "John", "Doe",
                "john@example.com", "+35312345678", address, "cus_abc123");
    }

    private PlanResponseDTO buildPlan() {
        return new PlanResponseDTO(UUID.randomUUID(), "Premium", "Premium Plan",
                new BigDecimal("9900"), "EUR", "month", "price_abc123", true);
    }

    private Notification buildNotificationWithSubscriptionCreated() {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusMonths(1),
                "PENDING", "pm_test", buildCustomer(), buildPlan());
        return Notification.builder()
                .from("noreply@billing.com")
                .to("john@example.com")
                .template(NotificationTemplate.SUBSCRIPTION_CREATED)
                .body(event)
                .build();
    }

    @Test
    void handlerNotificationTemplate_shouldBuildAndReturnEmailRequest_whenNotificationIsValid() {
        Notification notification = buildNotificationWithSubscriptionCreated();
        String processedTemplate = "<html>...</html>";
        SendEmailRequest expectedRequest = mock(SendEmailRequest.class);

        when(utils.loadTemplate(NotificationTemplate.SUBSCRIPTION_CREATED)).thenReturn("<html>{{customerFullName}}</html>");
        when(utils.buildEmailTemplate(any())).thenReturn(processedTemplate);
        when(utils.buildEmailRequest(notification, processedTemplate)).thenReturn(expectedRequest);

        SendEmailRequest result = templateEmailService.handlerNotificationTemplate(notification);

        assertThat(result).isEqualTo(expectedRequest);
        verify(utils).buildEmailRequest(notification, processedTemplate);
    }

    @Test
    void buildHtmlBody_shouldProcessSubscriptionCreatedEvent_whenBodyIsSubscriptionCreatedEvent() {
        Notification notification = buildNotificationWithSubscriptionCreated();
        String rawTemplate = "<html>{{customerFullName}}</html>";
        String processedTemplate = "<html>John</html>";

        when(utils.loadTemplate(NotificationTemplate.SUBSCRIPTION_CREATED)).thenReturn(rawTemplate);
        when(utils.buildEmailTemplate(any())).thenReturn(processedTemplate);

        String result = templateEmailService.buildHtmlBody(notification);

        assertThat(result).isEqualTo(processedTemplate);
    }

    @Test
    void buildHtmlBody_shouldProcessSubscriptionPaymentEvent_whenBodyIsSubscriptionPaymentEvent() {
        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .subscriptionId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .paymentStatus("APPROVED")
                .subscriptionStatus("ACTIVE")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer())
                .plan(buildPlan())
                .build();

        Notification notification = Notification.builder()
                .from("noreply@billing.com")
                .to("john@example.com")
                .template(NotificationTemplate.SUBSCRIPTION_PAID)
                .body(event)
                .build();

        when(utils.loadTemplate(NotificationTemplate.SUBSCRIPTION_PAID)).thenReturn("<html>...</html>");
        when(utils.buildEmailTemplate(any())).thenReturn("<html>Processed</html>");

        String result = templateEmailService.buildHtmlBody(notification);

        assertThat(result).isEqualTo("<html>Processed</html>");
    }

    @Test
    void buildHtmlBody_shouldProcessInvoiceCreatedEvent_whenBodyIsInvoiceCreatedEvent() {
        InvoiceCreatedEvent event = InvoiceCreatedEvent.builder()
                .invoiceId(UUID.randomUUID())
                .s3Key("invoices/test.pdf")
                .subscriptionId(UUID.randomUUID())
                .paymentStatus("APPROVED")
                .subscriptionStatus("ACTIVE")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer())
                .plan(buildPlan())
                .build();

        Notification notification = Notification.builder()
                .from("noreply@billing.com")
                .to("john@example.com")
                .template(NotificationTemplate.INVOICE_CREATED)
                .body(event)
                .build();

        when(utils.loadTemplate(NotificationTemplate.INVOICE_CREATED)).thenReturn("<html>...</html>");
        when(utils.buildEmailTemplate(any())).thenReturn("<html>Invoice</html>");

        String result = templateEmailService.buildHtmlBody(notification);

        assertThat(result).isEqualTo("<html>Invoice</html>");
    }

    @Test
    void buildHtmlBody_shouldThrowIllegalArgumentException_whenBodyTypeIsUnsupported() {
        Notification notification = Notification.builder()
                .from("noreply@billing.com")
                .to("john@example.com")
                .template(NotificationTemplate.SUBSCRIPTION_CREATED)
                .body("unsupported-body-type")
                .build();

        assertThatThrownBy(() -> templateEmailService.buildHtmlBody(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported notification body");
    }
}
