package com.billing.payment.service;

import com.billing.payment.controller.advice.exceptions.NotFoundException;
import com.billing.payment.events.data.CustomerClientResponse;
import com.billing.payment.events.data.PlanResponseDTO;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.events.data.SubscriptionPaymentEvent;
import com.billing.payment.events.publisher.PaymentEventPublisher;
import com.billing.payment.mapper.PaymentMapper;
import com.billing.payment.metrics.PaymentMetrics;
import com.billing.payment.model.AuditLog;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.repository.PaymentRepository;
import com.billing.payment.utils.PaymentUtils;
import com.billing.payment.validator.PaymentValidator;
import com.stripe.model.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private StripeSubscriptionService stripeSubscriptionService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private PaymentEventPublisher paymentEventPublisher;
    @Mock private PaymentValidator paymentValidator;
    @Mock private PaymentUtils paymentUtils;
    @Mock private PaymentMetrics paymentMetrics;

    @InjectMocks
    private PaymentService paymentService;

    private SubscriptionCreatedEvent buildSubscriptionEvent() {
        UUID subscriptionId = UUID.randomUUID();
        CustomerClientResponse customer = CustomerClientResponse.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@example.com")
                .build();
        PlanResponseDTO plan = PlanResponseDTO.builder()
                .price(new BigDecimal("9900"))
                .currency("EUR")
                .build();
        return new SubscriptionCreatedEvent(subscriptionId, LocalDate.now(),
                LocalDate.now().plusMonths(1), "PENDING", "pm_test", customer, plan);
    }

    private Payment buildPayment(UUID subscriptionId, PaymentStatus status) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .customerId(UUID.randomUUID())
                .amount(9900L)
                .currency("EUR")
                .paymentStatus(status)
                .auditLog(new AuditLog())
                .build();
    }

    @Test
    void processPayment_shouldSavePaymentAndCreateStripeSubscription_whenEventIsValid() {
        SubscriptionCreatedEvent event = buildSubscriptionEvent();
        Payment payment = buildPayment(event.subscriptionId(), PaymentStatus.PENDING);

        doNothing().when(paymentValidator).validateIdempotencyKey(event);
        when(paymentMapper.toEntity(event)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);
        doNothing().when(stripeSubscriptionService).createSubscription(event);

        paymentService.processPayment(event);

        verify(paymentValidator).validateIdempotencyKey(event);
        verify(paymentRepository).save(payment);
        verify(stripeSubscriptionService).createSubscription(event);
    }

    @Test
    void processPayment_shouldNotCreateStripeSubscription_whenValidationFails() {
        SubscriptionCreatedEvent event = buildSubscriptionEvent();
        doThrow(new RuntimeException("Duplicate payment")).when(paymentValidator).validateIdempotencyKey(event);

        assertThatThrownBy(() -> paymentService.processPayment(event))
                .isInstanceOf(RuntimeException.class);

        verify(paymentRepository, never()).save(any());
        verify(stripeSubscriptionService, never()).createSubscription(any());
    }

    @Test
    void handlerPaymentEvent_shouldProcessApprovedPayment_whenEventTypeIsInvoicePaid() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        Payment payment = buildPayment(subscriptionId, PaymentStatus.PENDING);
        Event stripeEvent = mock(Event.class);
        SubscriptionPaymentEvent paymentEvent = mock(SubscriptionPaymentEvent.class);
        String payload = "{\"type\":\"invoice.paid\"}";

        when(stripeEvent.getType()).thenReturn("invoice.paid");
        when(paymentUtils.getInternalSubscriptionId(stripeEvent)).thenReturn(subscriptionId);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toEvent(payment, stripeEvent, PaymentStatus.APPROVED)).thenReturn(paymentEvent);
        doNothing().when(paymentEventPublisher).publisherPaymentApproved(paymentEvent);

        paymentService.handlerPaymentEvent(stripeEvent, payload);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(paymentEventPublisher).publisherPaymentApproved(paymentEvent);
    }

    @Test
    void handlerPaymentEvent_shouldProcessFailedPayment_whenEventTypeIsInvoicePaymentFailed() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        Payment payment = buildPayment(subscriptionId, PaymentStatus.PENDING);
        Event stripeEvent = mock(Event.class);
        SubscriptionPaymentEvent paymentEvent = mock(SubscriptionPaymentEvent.class);
        String payload = "{\"type\":\"invoice.payment_failed\"}";

        when(stripeEvent.getType()).thenReturn("invoice.payment_failed");
        when(paymentUtils.getInternalSubscriptionId(stripeEvent)).thenReturn(subscriptionId);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toEvent(payment, stripeEvent, PaymentStatus.FAILED)).thenReturn(paymentEvent);
        doNothing().when(paymentEventPublisher).publisherPaymentFailed(paymentEvent);

        paymentService.handlerPaymentEvent(stripeEvent, payload);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentEventPublisher).publisherPaymentFailed(paymentEvent);
    }

    @Test
    void findPaymentBySubscriptionId_shouldReturnPayment_whenSubscriptionExists() {
        UUID subscriptionId = UUID.randomUUID();
        Payment payment = buildPayment(subscriptionId, PaymentStatus.PENDING);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(payment));

        Payment result = paymentService.findPaymentBySubscriptionId(subscriptionId);

        assertThat(result.getSubscriptionId()).isEqualTo(subscriptionId);
    }

    @Test
    void findPaymentBySubscriptionId_shouldThrowNotFoundException_whenSubscriptionDoesNotExist() {
        UUID subscriptionId = UUID.randomUUID();
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findPaymentBySubscriptionId(subscriptionId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No payment record found for subscription ID");
    }
}
