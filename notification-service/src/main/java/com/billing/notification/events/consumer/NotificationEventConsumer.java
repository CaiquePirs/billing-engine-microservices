package com.billing.notification.events.consumer;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.SnsMessage;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.events.tracing.MessagingTracing;
import com.billing.notification.metrics.NotificationMetrics;
import com.billing.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final NotificationMetrics notificationMetrics;
    private final MessagingTracing messagingTracing;

    @SqsListener("${aws.sqs.queue.notify-subscription}")
    public void processNewSubscriptionEvent(SnsMessage snsMessage) {
        messagingTracing.traceConsume("notify-new-subscription process", snsMessage, () -> {
            try {
                SubscriptionCreatedEvent event = objectMapper.readValue(
                        snsMessage.Message(),
                        SubscriptionCreatedEvent.class);

                notificationService.sendNewSubscriptionNotification(event);

                notificationMetrics.recordNewSubscriptionQueueMessageConsumedTotal();
                log.info("New-subscription notification event consumed from SQS (subscriptionId={})", event.subscriptionId());

            } catch (Exception e) {
                log.error("Failed to process new subscription notification event from SQS. Raw message: {}", snsMessage.Message(), e);
                notificationMetrics.recordNewSubscriptionQueueMessageConsumptionFailedTotal();
                throw new InternalNotificationErrorException("Failed to send new subscription notification email. Check event payload and SES configuration.", e);
            }
        });
    }

    @SqsListener("${aws.sqs.queue.notify-payment-approved}")
    public void processPaymentApprovedEvent(SnsMessage snsMessage) {
        messagingTracing.traceConsume("notify-payment-approved process", snsMessage, () -> {
            try {
                SubscriptionPaymentEvent event = objectMapper.readValue(
                        snsMessage.Message(),
                        SubscriptionPaymentEvent.class);

                notificationService.sendPaymentApprovedNotification(event);

                notificationMetrics.recordPaymentApprovedQueueMessageConsumedTotal();
                log.info("Payment-approved notification event consumed from SQS (subscriptionId={})", event.subscriptionId());

            } catch (Exception e) {
                log.error("Failed to process payment-approved notification event from SQS. Raw message: {}", snsMessage.Message(), e);
                notificationMetrics.recordPaymentApprovedQueueMessageConsumptionFailedTotal();
                throw new InternalNotificationErrorException("Failed to send payment-approved notification email.", e);
            }
        });
    }

    @SqsListener("${aws.sqs.queue.notify-payment-failed}")
    public void processPaymentFailedEvent(SnsMessage snsMessage) {
        messagingTracing.traceConsume("notify-payment-failed process", snsMessage, () -> {
            try {
                SubscriptionPaymentEvent event = objectMapper.readValue(
                        snsMessage.Message(),
                        SubscriptionPaymentEvent.class);

                notificationService.sendPaymentFailedNotification(event);

                notificationMetrics.recordPaymentFailedQueueMessageConsumedTotal();
                log.info("Payment-failed notification event consumed from SQS (subscriptionId={})", event.subscriptionId());

            } catch (Exception e) {
                log.error("Failed to process payment-failed notification event from SQS. Raw message: {}", snsMessage.Message(), e);
                notificationMetrics.recordPaymentFailedQueueMessageConsumptionFailedTotal();
                throw new InternalNotificationErrorException("Failed to send payment-failed notification email.", e);
            }
        });
    }

    @SqsListener("${aws.sqs.queue.invoice-created}")
    public void processInvoiceCreatedEvent(String rawMessage,
                                           @Header(name = "traceparent", required = false) String traceparent) {
        Map<String, String> headers = traceparent == null ? Map.of() : Map.of("traceparent", traceparent);
        messagingTracing.traceConsume("invoice-created process", headers, () -> {
            try {
                InvoiceCreatedEvent event = objectMapper.readValue(
                                rawMessage,
                                InvoiceCreatedEvent.class);

                notificationService.sendInvoiceCreatedNotification(event);

                notificationMetrics.recordInvoiceCreatedQueueMessageConsumedTotal();
                log.info("Invoice-created notification event consumed from SQS (invoiceId={})", event.invoiceId());

            } catch (Exception e) {
                log.error("Failed to process invoice-created notification event from SQS. Raw message: {}", rawMessage, e);
                notificationMetrics.recordInvoiceCreatedQueueMessageConsumptionFailedTotal();
                throw new InternalNotificationErrorException("Failed to send invoice-created notification email.", e);
            }
        });
    }
}
