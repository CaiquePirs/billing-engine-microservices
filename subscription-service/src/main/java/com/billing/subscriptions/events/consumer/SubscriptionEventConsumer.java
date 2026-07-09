package com.billing.subscriptions.events.consumer;

import com.billing.subscriptions.controller.advice.exception.ExternalServiceException;
import com.billing.subscriptions.events.data.SnsMessage;
import com.billing.subscriptions.events.data.SubscriptionPaymentEvent;
import com.billing.subscriptions.events.tracing.MessagingTracing;
import com.billing.subscriptions.metrics.BillingSubscriptionMetrics;
import com.billing.subscriptions.service.BillingSubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final BillingSubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;
    private final BillingSubscriptionMetrics billingSubscriptionMetrics;
    private final MessagingTracing messagingTracing;

    @SqsListener("${aws.sqs.queue.active-subscription}")
    public void activeSubscriptionPaid(SnsMessage snsMessage){
        messagingTracing.traceConsume("active-subscription process", snsMessage, () -> {
            try {
                SubscriptionPaymentEvent event = objectMapper.readValue(
                        snsMessage.Message(),
                        SubscriptionPaymentEvent.class
                );
                subscriptionService.activeSubscription(event);

                billingSubscriptionMetrics.recordActiveSubscriptionQueueMessageConsumedTotal();
                log.info("Activate-subscription event consumed from SQS (subscriptionId={})", event.subscriptionId());

            } catch (Exception e) {
                log.error("Failed to activate subscription from incoming SQS payment event. Raw message: {}", snsMessage.Message(), e);
                throw new ExternalServiceException("Failed to activate subscription from incoming SQS payment event.", e);
            }
        });
    }

    @SqsListener("${aws.sqs.queue.deactivate-subscription}")
    public void desactiveSubscription(SnsMessage snsMessage){
        messagingTracing.traceConsume("deactivate-subscription process", snsMessage, () -> {
            try {
                SubscriptionPaymentEvent event = objectMapper.readValue(
                        snsMessage.Message(),
                        SubscriptionPaymentEvent.class
                );
                subscriptionService.cancelSubscription(event);

                billingSubscriptionMetrics.recordDeactivateSubscriptionQueueConsumedTotal();
                log.info("Deactivate-subscription event consumed from SQS (subscriptionId={})", event.subscriptionId());

            } catch (Exception e) {
                log.error("Failed to cancel subscription from incoming SQS payment event. Raw message: {}", snsMessage.Message(), e);
                billingSubscriptionMetrics.recordDeactivateSubscriptionQueueMessageFailedTotal();
                throw new ExternalServiceException("Failed to cancel subscription from incoming SQS payment event.", e);
            }
        });
    }
}
