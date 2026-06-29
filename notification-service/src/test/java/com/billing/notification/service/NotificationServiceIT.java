package com.billing.notification.service;

import com.billing.notification.events.data.CustomerAddressResponse;
import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.PlanResponseDTO;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.strategy.port.SendNotificationPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.aws.sqs.enabled=false",
        "notification.sender-email=noreply@billing.test",
        "aws.localstack.access-key=test",
        "aws.localstack.secret-key=test",
        "aws.localstack.region=us-east-1",
        "aws.localstack.endpoint=http://localhost:4566",
        "aws.production.access-key=test",
        "aws.production.secret-key=test",
        "aws.production.region=us-east-1",
        "aws.s3.bucket=test-bucket"
})
class NotificationServiceIT {

    @Autowired private NotificationService notificationService;

    @MockBean private SendNotificationPort sendNotificationPort;
    @MockBean private InvoiceS3Service invoiceS3Service;

    private CustomerClientResponse buildCustomer() {
        CustomerAddressResponse address = new CustomerAddressResponse(
                UUID.randomUUID(), "Main St", "10", "Dublin", "Leinster", "Co. Dublin", "D01AB12");
        return new CustomerClientResponse(UUID.randomUUID(), "John", "Doe",
                "john@example.com", "+353123456789", address, "cus_test");
    }

    private PlanResponseDTO buildPlan() {
        return new PlanResponseDTO(UUID.randomUUID(), "Premium", "Premium plan",
                new BigDecimal("9900"), "EUR", "MONTHLY", "price_abc", true);
    }

    @Test
    void sendNewSubscriptionNotification_shouldMapEventAndInvokePort_whenEventIsValid() {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusMonths(1),
                "PENDING", "pm_test", buildCustomer(), buildPlan());
        doNothing().when(sendNotificationPort).sendEmail(any());

        notificationService.sendNewSubscriptionNotification(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(sendNotificationPort).sendEmail(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getTo()).isEqualTo("john@example.com");
        assertThat(notification.getFrom()).isEqualTo("noreply@billing.test");
        assertThat(notification.getBody()).isEqualTo(event);
    }

    @Test
    void sendPaymentApprovedNotification_shouldMapEventAndInvokePort_whenEventIsValid() {
        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .paymentId(UUID.randomUUID()).subscriptionId(UUID.randomUUID())
                .paymentStatus("APPROVED").subscriptionStatus("ACTIVE")
                .currentPeriodStart(LocalDate.now()).currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer()).plan(buildPlan()).build();
        doNothing().when(sendNotificationPort).sendEmail(any());

        notificationService.sendPaymentApprovedNotification(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(sendNotificationPort).sendEmail(captor.capture());

        assertThat(captor.getValue().getTo()).isEqualTo("john@example.com");
    }

    @Test
    void sendPaymentFailedNotification_shouldMapEventAndInvokePort_whenEventIsValid() {
        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .paymentId(UUID.randomUUID()).subscriptionId(UUID.randomUUID())
                .paymentStatus("FAILED").subscriptionStatus("CANCELED")
                .currentPeriodStart(LocalDate.now()).currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer()).plan(buildPlan()).build();
        doNothing().when(sendNotificationPort).sendEmail(any());

        notificationService.sendPaymentFailedNotification(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(sendNotificationPort).sendEmail(captor.capture());

        assertThat(captor.getValue().getTo()).isEqualTo("john@example.com");
    }

    @Test
    void sendInvoiceCreatedNotification_shouldDownloadPdfAndAttachToEmail_whenEventIsValid() {
        byte[] pdfBytes = "fake-pdf-content".getBytes();
        InvoiceCreatedEvent event = InvoiceCreatedEvent.builder()
                .invoiceId(UUID.randomUUID()).s3Key("invoices/test.pdf")
                .subscriptionId(UUID.randomUUID()).paymentStatus("APPROVED")
                .subscriptionStatus("ACTIVE").currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer()).plan(buildPlan()).status("GENERATED").build();

        when(invoiceS3Service.downloadPdf("invoices/test.pdf")).thenReturn(pdfBytes);
        doNothing().when(sendNotificationPort).sendEmail(any());

        notificationService.sendInvoiceCreatedNotification(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(sendNotificationPort).sendEmail(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getAttachmentBytes()).isEqualTo(pdfBytes);
        assertThat(notification.getAttachmentFileName()).contains(event.invoiceId().toString());
        assertThat(notification.getTo()).isEqualTo("john@example.com");
    }

    @Test
    void sendInvoiceCreatedNotification_shouldPropagateException_whenS3DownloadFails() {
        InvoiceCreatedEvent event = InvoiceCreatedEvent.builder()
                .invoiceId(UUID.randomUUID()).s3Key("invoices/missing.pdf")
                .subscriptionId(UUID.randomUUID()).paymentStatus("APPROVED")
                .subscriptionStatus("ACTIVE").currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer()).plan(buildPlan()).status("GENERATED").build();

        when(invoiceS3Service.downloadPdf("invoices/missing.pdf"))
                .thenThrow(new RuntimeException("S3 download failed"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> notificationService.sendInvoiceCreatedNotification(event))
                .isInstanceOf(RuntimeException.class);

        verify(sendNotificationPort, never()).sendEmail(any());
    }
}
