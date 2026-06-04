package com.billing.payment.events.publisher;

import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.mapper.PaymentMapper;
import com.billing.payment.model.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final PaymentMapper paymentMapper;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${PAYMENT_PROCESSING_DLQ}")
    private String dlqQueue;

    public void publisherPaymentApproved(Payment payment){}
    public void publisherPaymentFailed(Payment payment){}

    public void sendPaymentToDlqQueue(SubscriptionCreatedEvent event) {
        try {
            String eventMessage = objectMapper.writeValueAsString(event);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(dlqQueue)
                    .messageBody(eventMessage)
                    .build();

            sqsClient.sendMessage(request);

        } catch (Exception e) {
            log.error("Error while sending payment of subscriptionId {} to DLQ queue", event.id(), e);
        }
    }

}
