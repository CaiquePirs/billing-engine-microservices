package com.billing.payment.events.publisher;

import com.billing.payment.controller.advice.InternalErrorException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.data.SubscriptionPaymentEvent;
import com.billing.payment.mapper.PaymentMapper;
import com.billing.payment.model.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final PaymentMapper paymentMapper;
    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${PAYMENT_PROCESSING_DLQ}")
    private String dlqQueue;

    @Value("${PAYMENT_APPROVED_TOPIC}")
    private String snsTopicPaymentApproved;

    @Value("${PAYMENT_FAILED_TOPIC}")
    private String snsTopicPaymentFailed;

    public void publisherPaymentApproved(Payment payment) {
        SubscriptionPaymentEvent event = paymentMapper.toEvent(payment);

        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicPaymentApproved)
                    .message(objectMapper.writeValueAsString(event))
                    .build();

            snsClient.publish(request);

        } catch (Exception e) {
            log.error("Failed to publish payment-approved event for subscription ID {}", payment.getSubscriptionId(), e);
            throw new InternalErrorException("Failed to publish payment-approved event");
        }
    }

    public void publisherPaymentFailed(Payment payment){
        SubscriptionPaymentEvent event = paymentMapper.toEvent(payment);

        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicPaymentFailed)
                    .message(objectMapper.writeValueAsString(event))
                    .build();

            snsClient.publish(request);

        } catch (Exception e) {
            log.error("Failed to publish payment-failed event for subscription ID {}", payment.getSubscriptionId(), e);
            throw new InternalErrorException("Failed to publish payment-failed event");
        }
    }

    public void sendPaymentToDlqQueue(SubscriptionCreatedEvent event) {
        try {
            String eventMessage = objectMapper.writeValueAsString(event);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(dlqQueue)
                    .messageBody(eventMessage)
                    .build();

            sqsClient.sendMessage(request);

        } catch (Exception e) {
            log.error("Failed to publish subscriptionId {} to DLQ", event.id(), e);
            throw new InternalErrorException("Failed to publish subscription-created event to DLQ");
        }
    }

}
