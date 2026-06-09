package com.billing.payment.service;

import com.billing.payment.controller.advice.NotFoundException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.data.SubscriptionPaymentEvent;
import com.billing.payment.events.publisher.PaymentEventPublisher;
import com.billing.payment.mapper.PaymentMapper;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.repository.PaymentRepository;
import com.billing.payment.utils.PaymentUtils;
import com.billing.payment.validator.PaymentValidator;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final StripeSubscriptionService stripeSubscriptionService;
    private final StripePaymentMethodService stripePaymentMethodService;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentValidator paymentValidator;
    private final PaymentUtils paymentUtils;

    public void processPayment(SubscriptionCreatedEvent subscriptionEvent) {
        paymentValidator.validateIdempotencyKey(subscriptionEvent);

        String stripePaymentMethodId = stripePaymentMethodService.attachPaymentMethod(subscriptionEvent);
        Subscription subscription = stripeSubscriptionService.createSubscription(subscriptionEvent, stripePaymentMethodId);

        Payment payment = paymentMapper.toEntity(subscriptionEvent, subscription);
        paymentRepository.save(payment);
    }

    @Transactional
    public void handlerPaymentEvent(Event event, String payload) {
        switch (event.getType()) {
            case "invoice.paid" -> {
                try {
                    UUID subscriptionId = paymentUtils.getInternalSubscriptionId(event);
                    processPaymentApproved(payload, event, subscriptionId);

                }catch (Exception e) {
                    log.error("Failed to process Stripe invoice.paid webhook event ID {}", event.getId(), e);
                }

            }

            case "invoice.payment_failed" -> {
                try {
                    UUID subscriptionId = paymentUtils.getInternalSubscriptionId(event);
                    processPaymentFailed(payload, event, subscriptionId);

                } catch (Exception e){
                    log.error("Failed to process Stripe invoice.payment_failed webhook event ID {}", event.getId(), e);
                }
            }
        }
    }

    private void processPaymentApproved(String payload, Event webhookEvent, UUID subscriptionId) {
        Payment payment = findPaymentBySubscriptionId(subscriptionId);

        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setRawPayload(payload);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setStripeEventId(webhookEvent.getId());

        paymentRepository.save(payment);

        SubscriptionPaymentEvent event = paymentMapper.toEvent(payment, webhookEvent);
        paymentEventPublisher.publisherPaymentApproved(event);
    }

    private void processPaymentFailed(String payload, Event webhookEvent,  UUID subscriptionId) {
        Payment payment = findPaymentBySubscriptionId(subscriptionId);

        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setRawPayload(payload);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setStripeEventId(webhookEvent.getId());

        paymentRepository.save(payment);

        SubscriptionPaymentEvent event = paymentMapper.toEvent(payment, webhookEvent);
        paymentEventPublisher.publisherPaymentFailed(event);
    }

    public Payment findPaymentBySubscriptionId(UUID subscriptionId) {
        return paymentRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Payment not found for subscription id " + subscriptionId));
    }

}
