package com.billing.subscriptions.events.publisher;

import com.billing.subscriptions.controller.advice.exception.ExternalServiceException;
import com.billing.subscriptions.events.data.SubscriptionCreatedEvent;
import com.billing.subscriptions.metrics.BillingSubscriptionMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventPublisher {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final BillingSubscriptionMetrics billingSubscriptionMetrics;

    @Value("${sns.topic.subscription-created}")
    private String snsTopicSubscriptionCreated;

    public void publisherSubscriptionCreated(SubscriptionCreatedEvent subscriptionCreatedEvent) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicSubscriptionCreated)
                    .message(objectMapper.writeValueAsString(subscriptionCreatedEvent))
                    .build();

            snsClient.publish(request);
            billingSubscriptionMetrics.recordSubscriptionCreatedSnsPublishedTotal();
            log.info("Published subscription-created event to SNS (subscriptionId={})", subscriptionCreatedEvent.subscriptionId());

        } catch (Exception e) {
            log.error("Failed to publish subscription-created event to SNS (subscriptionId={})", subscriptionCreatedEvent.subscriptionId(), e);
            billingSubscriptionMetrics.recordSubscriptionCreatedSnsPublishFailedTotal();
            throw new ExternalServiceException("Failed to publish subscription-created SNS event for subscription ID: " + subscriptionCreatedEvent.subscriptionId());
        }
    }
}
