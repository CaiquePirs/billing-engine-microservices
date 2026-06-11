package com.billing.subscriptions.events.consumer;

import com.billing.subscriptions.controller.advice.exception.ExternalServiceException;
import com.billing.subscriptions.events.data.SnsMessage;
import com.billing.subscriptions.events.data.SubscriptionPaymentEvent;
import com.billing.subscriptions.service.BillingSubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.service.SubscriptionService;
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

    @SqsListener("${ACTIVE_SUBSCRIPTION_QUEUE}")
    public void activeSubscriptionPaid(SnsMessage snsMessage){
        try {
            SubscriptionPaymentEvent event = objectMapper.readValue(
                    snsMessage.Message(),
                    SubscriptionPaymentEvent.class
            );
            subscriptionService.activeSubscription(event);

        } catch (Exception e) {
            log.error("Failed to process subscription payment event {}", snsMessage.Message(), e);
            throw new ExternalServiceException("Failed to process subscription payment event.", e);
        }

    }

    @SqsListener("${DEACTIVATE_SUBSCRIPTION_QUEUE}")
    public void desactiveSubscription(SnsMessage snsMessage){
        try {
            SubscriptionPaymentEvent event = objectMapper.readValue(
                    snsMessage.Message(),
                    SubscriptionPaymentEvent.class
            );
            subscriptionService.cancelSubscription(event);

        } catch (Exception e) {
            log.error("Failed to process subscription payment event {}", snsMessage.Message(), e);
            throw new ExternalServiceException("Failed to process subscription payment event.", e);
        }

    }



}
