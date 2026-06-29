package com.billing.payment.mapper;

import com.billing.payment.events.data.CustomerClientResponse;
import com.billing.payment.events.data.PlanResponseDTO;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.billing.payment.model.AuditLog;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.service.StripeCustomerService;
import com.billing.payment.service.StripePlanService;
import com.billing.payment.utils.PaymentUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentMapperTest {

    @Mock private PaymentUtils paymentUtils;
    @Mock private StripePlanService stripePlanService;
    @Mock private StripeCustomerService stripeCustomerService;

    @InjectMocks
    private PaymentMapper paymentMapper;

    @Test
    void toEntity_shouldMapAllFields_whenSubscriptionEventIsValid() {
        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        CustomerClientResponse customer = CustomerClientResponse.builder()
                .id(customerId)
                .name("John")
                .email("john@example.com")
                .build();

        PlanResponseDTO plan = PlanResponseDTO.builder()
                .price(new BigDecimal("9900"))
                .currency("EUR")
                .build();

        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                subscriptionId, LocalDate.now(), LocalDate.now().plusMonths(1),
                "PENDING", "pm_test", customer, plan);

        Payment result = paymentMapper.toEntity(event);

        assertThat(result.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getAmount()).isEqualTo(9900L);
        assertThat(result.getCurrency()).isEqualTo("EUR");
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getAuditLog()).isNotNull();
    }

    @Test
    void toEntity_shouldSetPendingStatus_whenCreatingNewPayment() {
        CustomerClientResponse customer = CustomerClientResponse.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        PlanResponseDTO plan = PlanResponseDTO.builder()
                .price(new BigDecimal("1999"))
                .currency("USD")
                .build();

        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusMonths(1),
                "PENDING", "pm_xyz", customer, plan);

        Payment result = paymentMapper.toEntity(event);

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
