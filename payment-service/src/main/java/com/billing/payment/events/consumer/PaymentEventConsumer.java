package com.billing.payment.events.consumer;

import com.billing.payment.controller.advice.exceptions.InternalErrorException;
import com.billing.payment.events.data.SnsMessage;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
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
    private final ObjectMapper objectMapper;

    @SqsListener("${PROCESS_PAYMENT_QUEUE}")
    private void processNewPaymentEvent(SnsMessage snsMessage) {
        try {
            SubscriptionCreatedEvent event = objectMapper.readValue(
                            snsMessage.Message(),
                            SubscriptionCreatedEvent.class
                    );

            paymentService.processPayment(event);

        } catch (Exception e) {
            log.error("Failed to deserialize or process incoming SQS payment event. Raw message: {}", snsMessage.Message(), e);
            throw new InternalErrorException("Failed to process incoming payment event from SQS. The message could not be deserialized or handled.");
        }
    }
}
