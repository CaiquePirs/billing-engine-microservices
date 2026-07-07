package com.billing.payment.events.consumer;

import com.billing.payment.controller.advice.exceptions.InternalErrorException;
import com.billing.payment.events.data.SnsMessage;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.metrics.PaymentMetrics;
import com.billing.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final PaymentMetrics paymentMetrics;
    private final ObjectMapper objectMapper;

    @SqsListener("${aws.sqs.queue.process-payment}")
    private void processNewPaymentEvent(SnsMessage snsMessage) {
        try {
            SubscriptionCreatedEvent event = objectMapper.readValue(
                            snsMessage.Message(),
                            SubscriptionCreatedEvent.class
                    );

            paymentService.processPayment(event);
            paymentMetrics.recordProcessPaymentQueueMessageConsumedTotal();
            log.info("Payment event consumed from SQS (subscriptionId={})", event.subscriptionId());

        } catch (Exception e) {
            log.error("Failed to deserialize or process incoming SQS payment event. Raw message: {}", snsMessage.Message(), e);
            paymentMetrics.recordProcessPaymentQueueMessageConsumptionFailedTotal();
            throw new InternalErrorException("Failed to process incoming payment event from SQS. The message could not be deserialized or handled.");
        }
    }
}
