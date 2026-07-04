package com.billing.notification.service;

import com.billing.notification.events.data.CustomerAddressResponse;
import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.PlanResponseDTO;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.mapper.NotificationMapper;
import com.billing.notification.metrics.NotificationMetrics;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import com.billing.notification.strategy.port.SendNotificationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private SendNotificationPort sendNotificationPort;
    @Mock private NotificationMapper notificationMapper;
    @Mock private InvoiceS3Service invoiceS3Service;
    @Mock private NotificationMetrics notificationMetrics;

    @InjectMocks
    private NotificationService notificationService;

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

    @Test
    void sendNewSubscriptionNotification_shouldMapAndSendEmail_whenEventIsValid() {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusMonths(1),
                "PENDING", "pm_test", buildCustomer(), buildPlan());

        Notification notification = Notification.builder()
                .to("john@example.com")
                .from("noreply@billing.com")
                .template(NotificationTemplate.SUBSCRIPTION_CREATED)
                .body(event)
                .build();

        when(notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_CREATED)).thenReturn(notification);

        notificationService.sendNewSubscriptionNotification(event);

        verify(notificationMapper).mapToNotification(event, NotificationTemplate.SUBSCRIPTION_CREATED);
        verify(sendNotificationPort).sendEmail(notification);
        verify(notificationMetrics).recordSubscriptionCreatedEmailSentTotal();
    }

    @Test
    void sendPaymentApprovedNotification_shouldMapAndSendEmail_whenEventIsValid() {
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
                .to("john@example.com")
                .from("noreply@billing.com")
                .template(NotificationTemplate.SUBSCRIPTION_PAID)
                .body(event)
                .build();

        when(notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_PAID)).thenReturn(notification);

        notificationService.sendPaymentApprovedNotification(event);

        verify(notificationMapper).mapToNotification(event, NotificationTemplate.SUBSCRIPTION_PAID);
        verify(sendNotificationPort).sendEmail(notification);
        verify(notificationMetrics).recordPaymentApprovedEmailSentTotal();
    }

    @Test
    void sendPaymentFailedNotification_shouldMapAndSendEmail_whenEventIsValid() {
        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .subscriptionId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .paymentStatus("FAILED")
                .subscriptionStatus("CANCELED")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer())
                .plan(buildPlan())
                .build();

        Notification notification = Notification.builder()
                .to("john@example.com")
                .from("noreply@billing.com")
                .template(NotificationTemplate.SUBSCRIPTION_CANCELLED)
                .body(event)
                .build();

        when(notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_CANCELLED)).thenReturn(notification);

        notificationService.sendPaymentFailedNotification(event);

        verify(notificationMapper).mapToNotification(event, NotificationTemplate.SUBSCRIPTION_CANCELLED);
        verify(sendNotificationPort).sendEmail(notification);
        verify(notificationMetrics).recordPaymentFailedEmailSentTotal();
    }

    @Test
    void sendInvoiceCreatedNotification_shouldDownloadPdfAndSendEmailWithAttachment_whenEventIsValid() {
        UUID invoiceId = UUID.randomUUID();
        String s3Key = "invoices/invoice-abc.pdf";
        byte[] pdfBytes = "pdf-content".getBytes();

        InvoiceCreatedEvent event = InvoiceCreatedEvent.builder()
                .invoiceId(invoiceId)
                .s3Key(s3Key)
                .subscriptionId(UUID.randomUUID())
                .paymentStatus("APPROVED")
                .subscriptionStatus("ACTIVE")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer())
                .plan(buildPlan())
                .build();

        Notification notification = Notification.builder()
                .to("john@example.com")
                .from("noreply@billing.com")
                .template(NotificationTemplate.INVOICE_CREATED)
                .body(event)
                .build();

        when(invoiceS3Service.downloadPdf(s3Key)).thenReturn(pdfBytes);
        when(notificationMapper.mapToNotification(event, NotificationTemplate.INVOICE_CREATED)).thenReturn(notification);

        notificationService.sendInvoiceCreatedNotification(event);

        verify(invoiceS3Service).downloadPdf(s3Key);
        verify(sendNotificationPort).sendEmail(notification);
        verify(notificationMetrics).recordInvoiceCreatedEmailSentTotal();
        assert notification.getAttachmentBytes() == pdfBytes;
        assert notification.getAttachmentFileName().equals("invoice-" + invoiceId + ".pdf");
    }
}
