package com.billing.payment.validator;

import com.billing.payment.controller.advice.exceptions.InternalErrorException;
import com.billing.payment.events.data.CustomerClientResponse;
import com.billing.payment.events.data.PlanResponseDTO;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.model.AuditLog;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentValidatorTest {

    @Mock private PaymentRepository paymentRepository;

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

        assertThatCode(() -> paymentValidator.validateIdempotencyKey(event))
                .doesNotThrowAnyException();
    }

    @Test
    void validateIdempotencyKey_shouldNotThrow_whenExistingPaymentIsPending() {
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionCreatedEvent event = buildEvent(subscriptionId);
        Payment pendingPayment = buildPayment(subscriptionId, PaymentStatus.PENDING);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(pendingPayment));

        assertThatCode(() -> paymentValidator.validateIdempotencyKey(event))
                .doesNotThrowAnyException();
    }

    @Test
    void validateIdempotencyKey_shouldThrowInternalErrorException_whenNonPendingPaymentAlreadyExists() {
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionCreatedEvent event = buildEvent(subscriptionId);
        Payment approvedPayment = buildPayment(subscriptionId, PaymentStatus.APPROVED);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(approvedPayment));

        assertThatThrownBy(() -> paymentValidator.validateIdempotencyKey(event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Duplicate payment detected");
    }

    @Test
    void validateIdempotencyKey_shouldThrowInternalErrorException_whenFailedPaymentAlreadyExists() {
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionCreatedEvent event = buildEvent(subscriptionId);
        Payment failedPayment = buildPayment(subscriptionId, PaymentStatus.FAILED);
        when(paymentRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(failedPayment));

        assertThatThrownBy(() -> paymentValidator.validateIdempotencyKey(event))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Duplicate payment detected");
    }
}
