package com.billing.notification.mapper;

import com.billing.notification.events.data.CustomerAddressResponse;
import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.InvoiceCreatedEvent;
import com.billing.notification.events.data.PlanResponseDTO;
import com.billing.notification.events.data.SubscriptionCreatedEvent;
import com.billing.notification.events.data.SubscriptionPaymentEvent;
import com.billing.notification.model.Notification;
import com.billing.notification.model.NotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final NotificationMapper notificationMapper = new NotificationMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationMapper, "internalEmail", "noreply@billing.com");
    }

    private CustomerClientResponse buildCustomer() {
        CustomerAddressResponse address = new CustomerAddressResponse(
                UUID.randomUUID(), "Main St", "10", "Dublin", "Leinster", "Ireland", "D01 AB12");
        return new CustomerClientResponse(UUID.randomUUID(), "John", "Doe",
                "john@example.com", "+35312345678", address, "cus_abc123");
    }

    private PlanResponseDTO buildPlan() {
        return new PlanResponseDTO(UUID.randomUUID(), "Premium", "Premium Plan",
                new BigDecimal("9900"), "EUR", "month", "price_abc123", true);
    }

    @Test
    void mapToNotification_shouldMapSubscriptionCreatedEvent_withCorrectTemplate() {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusMonths(1),
                "PENDING", "pm_test", buildCustomer(), buildPlan());

        Notification result = notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_CREATED);

        assertThat(result.getTo()).isEqualTo("john@example.com");
        assertThat(result.getFrom()).isEqualTo("noreply@billing.com");
        assertThat(result.getTemplate()).isEqualTo(NotificationTemplate.SUBSCRIPTION_CREATED);
        assertThat(result.getBody()).isEqualTo(event);
    }

    @Test
    void mapToNotification_shouldMapInvoiceCreatedEvent_withCorrectTemplate() {
        InvoiceCreatedEvent event = InvoiceCreatedEvent.builder()
                .invoiceId(UUID.randomUUID())
                .s3Key("invoices/test.pdf")
                .subscriptionId(UUID.randomUUID())
                .paymentStatus("APPROVED")
                .subscriptionStatus("ACTIVE")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer())
                .plan(buildPlan())
                .build();

        Notification result = notificationMapper.mapToNotification(event, NotificationTemplate.INVOICE_CREATED);

        assertThat(result.getTo()).isEqualTo("john@example.com");
        assertThat(result.getFrom()).isEqualTo("noreply@billing.com");
        assertThat(result.getTemplate()).isEqualTo(NotificationTemplate.INVOICE_CREATED);
        assertThat(result.getBody()).isEqualTo(event);
    }

    @Test
    void mapToNotification_shouldMapSubscriptionPaymentEvent_withSubscriptionPaidTemplate() {
        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .subscriptionId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .paymentStatus("APPROVED")
                .subscriptionStatus("ACTIVE")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer())
                .plan(buildPlan())
                .build();

        Notification result = notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_PAID);

        assertThat(result.getTo()).isEqualTo("john@example.com");
        assertThat(result.getFrom()).isEqualTo("noreply@billing.com");
        assertThat(result.getTemplate()).isEqualTo(NotificationTemplate.SUBSCRIPTION_PAID);
    }

    @Test
    void mapToNotification_shouldMapSubscriptionPaymentEvent_withSubscriptionCancelledTemplate() {
        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .subscriptionId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .paymentStatus("FAILED")
                .subscriptionStatus("CANCELED")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(buildCustomer())
                .plan(buildPlan())
                .build();

        Notification result = notificationMapper.mapToNotification(event, NotificationTemplate.SUBSCRIPTION_CANCELLED);

        assertThat(result.getTo()).isEqualTo("john@example.com");
        assertThat(result.getTemplate()).isEqualTo(NotificationTemplate.SUBSCRIPTION_CANCELLED);
    }
}
