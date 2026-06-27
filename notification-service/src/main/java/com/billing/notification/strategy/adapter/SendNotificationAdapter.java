package com.billing.notification.strategy.adapter;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import com.billing.notification.model.Notification;
import com.billing.notification.service.TemplateEmailService;
import com.billing.notification.strategy.port.SendNotificationPort;
import com.billing.notification.utils.TemplateNotificationUtils;
import jakarta.activation.DataHandler;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendNotificationAdapter implements SendNotificationPort {

    private final SesClient sesClient;
    private final TemplateEmailService templateEmailService;
    private final TemplateNotificationUtils utils;

    @Override
    public void sendEmail(Notification notification) {
        if (notification.getAttachmentBytes() != null) {
            sendEmailWithAttachment(notification);
        } else {
            sendSimpleEmail(notification);
        }
    }

    private void sendSimpleEmail(Notification notification) {
        try {
            SendEmailRequest request = templateEmailService.handlerNotificationTemplate(notification);
            sesClient.sendEmail(request);

        } catch (Exception e) {
            log.error("Failed to send email via AWS SES. Template: '{}', Recipient: '{}'", notification.getTemplate(), notification.getTo(), e);
            throw new InternalNotificationErrorException("Failed to deliver email notification via AWS SES for recipient: " + notification.getTo(), e);
        }
    }

    private void sendEmailWithAttachment(Notification notification) {
        try {
            String htmlBody = templateEmailService.buildHtmlBody(notification);
            String subject = utils.buildEmailTitle(notification.getTemplate());

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(notification.getFrom()));
            mimeMessage.setRecipient(RecipientType.TO, new InternetAddress(notification.getTo()));
            mimeMessage.setSubject(subject, "UTF-8");

            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setDataHandler(new DataHandler(
                    new ByteArrayDataSource(notification.getAttachmentBytes(), "application/pdf")));
            attachmentPart.setFileName(notification.getAttachmentFileName());
            multipart.addBodyPart(attachmentPart);

            mimeMessage.setContent(multipart);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);

            sesClient.sendRawEmail(SendRawEmailRequest.builder()
                    .rawMessage(RawMessage.builder()
                            .data(SdkBytes.fromByteArray(outputStream.toByteArray()))
                            .build())
                    .build());

        } catch (Exception e) {
            log.error("Failed to send email with attachment via AWS SES. Template: '{}', Recipient: '{}'", notification.getTemplate(), notification.getTo(), e);
            throw new InternalNotificationErrorException("Failed to deliver invoice email with PDF attachment via AWS SES for recipient: " + notification.getTo(), e);
        }
    }
}
