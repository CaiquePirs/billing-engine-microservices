package com.billing.payment.mapper;

import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.data.SubscriptionPaymentEvent;
import com.billing.payment.model.AuditLog;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.utils.PaymentUtils;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private final PaymentUtils paymentUtils;

    public Payment toEntity(SubscriptionCreatedEvent subscriptionEvent, Subscription subscription) {
        return Payment.builder()
                .subscriptionId(subscriptionEvent.id())
                .customerId(subscriptionEvent.customer().id())
                .amount(subscriptionEvent.plan().price().longValue())
                .currency(subscriptionEvent.plan().currency())
                .paymentStatus(PaymentStatus.PENDING)
                .auditLog(new AuditLog())
                .build();
    }

    public SubscriptionPaymentEvent toEvent(Payment payment, Event event) {
        return SubscriptionPaymentEvent.builder()
                .stripeSubscriptionId(paymentUtils.getStripeSubscriptionId(event))
                .currentPeriodStart(paymentUtils.getCurrentPeriodStart(event))
                .currentPeriodEnd(paymentUtils.getCurrentPeriodEnd(event))
                .subscriptionId(payment.getSubscriptionId())
                .paymentId(payment.getId())
                .paymentStatus(payment.getPaymentStatus().toString())
                .processedAt(payment.getProcessedAt())
                .build();
    }
}
