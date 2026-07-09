package com.billing.payment.validator;

import com.billing.payment.controller.advice.exceptions.InternalErrorException;
import com.billing.payment.events.data.CustomerClientResponse;
import com.billing.payment.events.data.PlanResponseDTO;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.metrics.PaymentMetrics;
import com.billing.payment.model.AuditLog;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.repository.PaymentRepository;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentValidatorTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMetrics paymentMetrics;

    @InjectMocks
    private PaymentValidator paymentValidator;

    private SubscriptionCreatedEvent buildEvent(UUID subscriptionId) {
        CustomerClientResponse customer = CustomerClientResponse.builder()
                .id(UUID.randomUUID()).email("john@example.com").build();
        PlanResponseDTO plan = PlanResponseDTO.builder()
                .price(new BigDecimal("9900")).currency("EUR").build();
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
    void validateIdempotencyKey_shouldNotThrow_whenNoPaymentExistsForSubscription() {
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionCreatedEvent event = buildEvent(subscriptionId);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.empty());

        assertThatCode(() -> paymentValidator.validateIdempotencyKeyBySubscriptionId(event))
                .doesNotThrowAnyException();
    }

    @Test
    void validateIdempotencyKey_shouldNotThrow_whenExistingPaymentIsPending() {
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionCreatedEvent event = buildEvent(subscriptionId);
        Payment pendingPayment = buildPayment(subscriptionId, PaymentStatus.PENDING);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(pendingPayment));

        assertThatCode(() -> paymentValidator.validateIdempotencyKeyBySubscriptionId(event))
                .doesNotThrowAnyException();
    }

    @Test
    void validateIdempotencyKey_shouldThrowInternalErrorException_whenNonPendingPaymentAlreadyExists() {
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionCreatedEvent event = buildEvent(subscriptionId);
        Payment approvedPayment = buildPayment(subscriptionId, PaymentStatus.APPROVED);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(approvedPayment));

        assertThatThrownBy(() -> paymentValidator.validateIdempotencyKeyBySubscriptionId(event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Duplicate payment detected");
    }

    @Test
    void validateIdempotencyKey_shouldThrowInternalErrorException_whenFailedPaymentAlreadyExists() {
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionCreatedEvent event = buildEvent(subscriptionId);
        Payment failedPayment = buildPayment(subscriptionId, PaymentStatus.FAILED);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(failedPayment));

        assertThatThrownBy(() -> paymentValidator.validateIdempotencyKeyBySubscriptionId(event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Duplicate payment detected");
    }

    @Test
    void validateIdempotencyKeyByStripeEvent_shouldReturnTrueAndRecordMetric_whenEventIdAlreadyProcessed() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_duplicate");
        when(paymentRepository.existsByStripeEventId("evt_duplicate")).thenReturn(true);

        boolean duplicate = paymentValidator.validateIdempotencyKeyByStripeEvent(event);

        assertThat(duplicate).isTrue();
        verify(paymentMetrics).recordStripeWebhookDuplicateIgnoredTotal();
    }

    @Test
    void validateIdempotencyKeyByStripeEvent_shouldReturnFalse_whenEventIdNotProcessedYet() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_new");
        when(paymentRepository.existsByStripeEventId("evt_new")).thenReturn(false);

        boolean duplicate = paymentValidator.validateIdempotencyKeyByStripeEvent(event);

        assertThat(duplicate).isFalse();
        verify(paymentMetrics, never()).recordStripeWebhookDuplicateIgnoredTotal();
    }
}
