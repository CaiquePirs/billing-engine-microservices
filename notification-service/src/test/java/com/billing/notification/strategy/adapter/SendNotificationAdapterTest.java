package com.billing.notification.strategy.adapter;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import com.billing.notification.metrics.NotificationMetrics;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import com.billing.notification.service.TemplateEmailService;
import com.billing.notification.utils.TemplateNotificationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendNotificationAdapterTest {

    @Mock private SesClient sesClient;
    @Mock private TemplateEmailService templateEmailService;
    @Mock private TemplateNotificationUtils utils;
    @Mock private NotificationMetrics notificationMetrics;

    @InjectMocks
    private SendNotificationAdapter sendNotificationAdapter;

    private Notification buildNotification() {
        return Notification.builder()
                .from("noreply@billing.com")
                .to("john@example.com")
                .template(NotificationTemplate.SUBSCRIPTION_CREATED)
                .build();
    }

    private Notification buildNotificationWithAttachment() {
        Notification notification = buildNotification();
        notification.setTemplate(NotificationTemplate.INVOICE_CREATED);
        notification.setAttachmentBytes("pdf-content".getBytes());
        notification.setAttachmentFileName("invoice-123.pdf");
        return notification;
    }

    @Test
    void sendEmail_shouldSendSimpleEmailAndRecordDeliverySuccess_whenNotificationHasNoAttachment() {
        Notification notification = buildNotification();
        SendEmailRequest request = SendEmailRequest.builder().build();
        when(templateEmailService.handlerNotificationTemplate(notification)).thenReturn(request);

        sendNotificationAdapter.sendEmail(notification);

        verify(sesClient).sendEmail(request);
        verify(notificationMetrics).recordEmailDeliverySucceededTotal();
        verify(notificationMetrics, never()).recordEmailWithAttachmentSentTotal();
    }

    @Test
    void sendEmail_shouldThrowInternalNotificationErrorExceptionAndRecordDeliveryFailure_whenSesClientFailsWithoutAttachment() {
        Notification notification = buildNotification();
        SendEmailRequest request = SendEmailRequest.builder().build();
        when(templateEmailService.handlerNotificationTemplate(notification)).thenReturn(request);
        when(sesClient.sendEmail(request)).thenThrow(new RuntimeException("SES error"));

        assertThatThrownBy(() -> sendNotificationAdapter.sendEmail(notification))
                .isInstanceOf(InternalNotificationErrorException.class)
                .hasMessageContaining("Failed to deliver email notification via AWS SES");

        verify(notificationMetrics).recordEmailDeliveryFailedTotal();
        verify(notificationMetrics, never()).recordEmailDeliverySucceededTotal();
    }

    @Test
    void sendEmail_shouldSendRawEmailAndRecordAttachmentAndDeliverySuccess_whenNotificationHasAttachment() {
        Notification notification = buildNotificationWithAttachment();
        when(templateEmailService.buildHtmlBody(notification)).thenReturn("<html>body</html>");
        when(utils.buildEmailTitle(NotificationTemplate.INVOICE_CREATED)).thenReturn("Your Invoice is Ready");

        sendNotificationAdapter.sendEmail(notification);

        verify(sesClient).sendRawEmail(any(SendRawEmailRequest.class));
        verify(notificationMetrics).recordEmailDeliverySucceededTotal();
        verify(notificationMetrics).recordEmailWithAttachmentSentTotal();
    }

    @Test
    void sendEmail_shouldThrowInternalNotificationErrorExceptionAndRecordDeliveryFailure_whenSesClientFailsWithAttachment() {
        Notification notification = buildNotificationWithAttachment();
        when(templateEmailService.buildHtmlBody(notification)).thenReturn("<html>body</html>");
        when(utils.buildEmailTitle(NotificationTemplate.INVOICE_CREATED)).thenReturn("Your Invoice is Ready");
        when(sesClient.sendRawEmail(any(SendRawEmailRequest.class))).thenThrow(new RuntimeException("SES error"));

        assertThatThrownBy(() -> sendNotificationAdapter.sendEmail(notification))
                .isInstanceOf(InternalNotificationErrorException.class)
                .hasMessageContaining("Failed to deliver invoice email with PDF attachment via AWS SES");

        verify(notificationMetrics).recordEmailDeliveryFailedTotal();
        verify(notificationMetrics, never()).recordEmailWithAttachmentSentTotal();
    }
}
