package com.billing.payment.validator;

import com.billing.payment.controller.advice.exceptions.InternalErrorException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.metrics.PaymentMetrics;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.repository.PaymentRepository;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentValidator {

    private final PaymentRepository paymentRepository;
    private final PaymentMetrics paymentMetrics;

    public void validateIdempotencyKeyBySubscriptionId(SubscriptionCreatedEvent subscriptionEvent) {
        paymentRepository.findBySubscriptionId(subscriptionEvent.subscriptionId())
                .filter(payment -> !payment.getPaymentStatus().equals(PaymentStatus.PENDING))
                .ifPresent(payment -> {
                    log.error("Payment already exists for subscription id {}", subscriptionEvent.subscriptionId());
                    throw new InternalErrorException("Duplicate payment detected: a non-pending payment already exists for subscription ID: " + subscriptionEvent.subscriptionId());
                });
    }

    public boolean validateIdempotencyKeyByStripeEvent(Event event) {
        if (paymentRepository.existsByStripeEventId(event.getId())) {
            paymentMetrics.recordStripeWebhookDuplicateIgnoredTotal();
            log.info("Duplicate Stripe webhook event ignored (eventId={}, type={})", event.getId(), event.getType());
            return true;
        }
        return false;
    }

}
