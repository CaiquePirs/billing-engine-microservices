package com.billing.payment.service;

import com.billing.payment.controller.advice.exceptions.NotFoundException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.data.SubscriptionPaymentEvent;
import com.billing.payment.events.publisher.PaymentEventPublisher;
import com.billing.payment.mapper.PaymentMapper;
import com.billing.payment.metrics.PaymentMetrics;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.repository.PaymentRepository;
import com.billing.payment.utils.PaymentUtils;
import com.billing.payment.validator.PaymentValidator;
import com.stripe.model.Event;
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
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentValidator paymentValidator;
    private final PaymentUtils paymentUtils;
    private final PaymentMetrics paymentMetrics;

    public void processPayment(SubscriptionCreatedEvent subscriptionEvent) {
        paymentValidator.validateIdempotencyKeyBySubscriptionId(subscriptionEvent)
                .orElseGet(() -> paymentRepository.save(paymentMapper.toEntity(subscriptionEvent)));

        stripeSubscriptionService.createSubscription(subscriptionEvent);

        paymentMetrics.recordStripeSubscriptionChargeSubmittedTotal();
        log.info("Payment charge submitted to Stripe (subscriptionId={})", subscriptionEvent.subscriptionId());
    }

    @Transactional
    public void handlerPaymentEvent(Event event, String payload) {
        if (paymentValidator.validateIdempotencyKeyByStripeEvent(event)) {
            return;
        }

        switch (event.getType()) {
            case "invoice.paid" -> {
                try {
                    UUID subscriptionId = paymentUtils.getInternalSubscriptionId(event);
                    processPaymentApproved(payload, event, subscriptionId);

                    paymentMetrics.recordInvoicePaidWebhookHandledTotal();

                }catch (Exception e) {
                    paymentMetrics.recordInvoicePaidWebhookHandlingFailedTotal();
                    log.error("Failed to process Stripe invoice.paid webhook event ID {}", event.getId(), e);
                }
            }

            case "invoice.payment_failed" -> {
                try {
                    UUID subscriptionId = paymentUtils.getInternalSubscriptionId(event);
                    processPaymentFailed(payload, event, subscriptionId);

                    paymentMetrics.recordInvoicePaymentFailedWebhookHandledTotal();

                } catch (Exception e){
                    paymentMetrics.recordInvoicePaymentFailedWebhookHandlingFailedTotal();
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

        SubscriptionPaymentEvent event = paymentMapper.toEvent(payment, webhookEvent, PaymentStatus.APPROVED);
        paymentEventPublisher.publisherPaymentApproved(event);

        paymentMetrics.recordPaymentApprovedOutcomeTotal();
        log.info("Payment approved (subscriptionId={}, paymentId={})", subscriptionId, payment.getId());
    }

    private void processPaymentFailed(String payload, Event webhookEvent,  UUID subscriptionId) {
        Payment payment = findPaymentBySubscriptionId(subscriptionId);

        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setRawPayload(payload);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setStripeEventId(webhookEvent.getId());

        paymentRepository.save(payment);

        SubscriptionPaymentEvent event = paymentMapper.toEvent(payment, webhookEvent, PaymentStatus.FAILED);
        paymentEventPublisher.publisherPaymentFailed(event);

        paymentMetrics.recordPaymentFailedOutcomeTotal();
        log.warn("Payment failed (subscriptionId={}, paymentId={})", subscriptionId, payment.getId());
    }

    public Payment findPaymentBySubscriptionId(UUID subscriptionId) {
        return paymentRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> {
                    log.warn("Payment lookup failed: no payment record found (subscriptionId={})", subscriptionId);
                    return new NotFoundException("No payment record found for subscription ID: " + subscriptionId);
                });
    }

}
