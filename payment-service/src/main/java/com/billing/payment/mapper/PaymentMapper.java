package com.billing.payment.mapper;

import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.data.SubscriptionPaymentEvent;
import com.billing.payment.model.AuditLog;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.stripe.model.Subscription;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class PaymentMapper {

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

    public SubscriptionPaymentEvent toEvent(Payment payment) {
        return SubscriptionPaymentEvent.builder()
                .subscriptionId(payment.getSubscriptionId())
                .paymentId(payment.getId())
                .paymentStatus(payment.getPaymentStatus().toString())
                .processedAt(payment.getProcessedAt())
                .build();
    }
}
