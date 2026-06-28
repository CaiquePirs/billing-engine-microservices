package com.billing.payment.events.publisher;

import com.billing.payment.controller.advice.exceptions.InternalErrorException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.data.SubscriptionPaymentEvent;
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

    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue.payment-processing-dlq}")
    private String dlqQueue;

    @Value("${aws.sns.topic.payment-approved}")
    private String snsTopicPaymentApproved;

    @Value("${aws.sns.topic.payment-failed}")
    private String snsTopicPaymentFailed;

    public void publisherPaymentApproved(SubscriptionPaymentEvent event) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicPaymentApproved)
                    .message(objectMapper.writeValueAsString(event))
                    .build();

            snsClient.publish(request);

        } catch (Exception e) {
            log.error("Failed to publish payment-approved event for subscription ID {}", event.subscriptionId(), e);
            throw new InternalErrorException("Failed to publish payment-approved SNS event for subscription ID: " + event.subscriptionId());
        }
    }

    public void publisherPaymentFailed( SubscriptionPaymentEvent event){
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicPaymentFailed)
                    .message(objectMapper.writeValueAsString(event))
                    .build();

            snsClient.publish(request);

        } catch (Exception e) {
            log.error("Failed to publish payment-failed event for subscription ID {}", event.subscriptionId(), e);
            throw new InternalErrorException("Failed to publish payment-failed SNS event for subscription ID: " + event.subscriptionId());
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
            log.error("Failed to publish subscriptionId {} to DLQ", event.subscriptionId(), e);
            throw new InternalErrorException("Failed to forward subscription ID: " + event.subscriptionId() + " to DLQ after payment processing failure");
        }
    }

}
