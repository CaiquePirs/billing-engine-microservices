package com.billing.payment.validator;

import com.billing.payment.controller.advice.exceptions.InternalErrorException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentValidator {

    private final PaymentRepository paymentRepository;

    public void validateIdempotencyKey(SubscriptionCreatedEvent subscriptionEvent) {
        paymentRepository.findBySubscriptionId(subscriptionEvent.subscriptionId())
                .filter(payment -> !payment.getPaymentStatus().equals(PaymentStatus.PENDING))
                .ifPresent(payment -> {
                    log.error("Payment already exists for subscription id {}", subscriptionEvent.subscriptionId());
                    throw new InternalErrorException("Payment already exists for subscription Id: " + subscriptionEvent.subscriptionId());
                });
    }
}
