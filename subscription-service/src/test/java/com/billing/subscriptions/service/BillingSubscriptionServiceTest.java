package com.billing.subscriptions.service;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.advice.exception.NotFoundException;
import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
import com.billing.subscriptions.events.data.SubscriptionCreatedEvent;
import com.billing.subscriptions.events.data.SubscriptionPaymentEvent;
import com.billing.subscriptions.events.publisher.SubscriptionEventPublisher;
import com.billing.subscriptions.mapper.BillingSubscriptionMapper;
import com.billing.subscriptions.model.AuditLog;
import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.model.enums.IntervalPlan;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import com.billing.subscriptions.repository.BillingSubscriptionRepository;
import com.billing.subscriptions.validator.SubscriptionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingSubscriptionServiceTest {

    @Mock private PlanService planService;
    @Mock private CustomerApiService customerApiService;
    @Mock private BillingSubscriptionRepository billingSubscriptionRepository;
    @Mock private BillingSubscriptionMapper billingSubscriptionMapper;
    @Mock private StripeCustomerService stripeCustomerService;
    @Mock private StripePlanService stripePlanService;
    @Mock private SubscriptionEventPublisher subscriptionEventPublisher;
    @Mock private SubscriptionValidator subscriptionValidator;

    @InjectMocks
    private BillingSubscriptionService billingSubscriptionService;

    private Plan buildPlan() {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("Premium")
                .description("Premium plan")
                .price(new BigDecimal("99.00"))
                .currency("EUR")
                .interval(IntervalPlan.MONTHLY)
                .stripePriceId("price_abc123")
                .active(true)
                .createdBy(UUID.randomUUID())
                .auditLog(new AuditLog())
                .build();
    }

    private CustomerClientResponse buildCustomer() {
        return new CustomerClientResponse(UUID.randomUUID(), "John", "Doe",
                "john@example.com", "+35312345678", "123456789", 34,
                LocalDate.of(1990, 1, 1), null, "ACTIVE", "cus_abc123");
    }

    private BillingSubscription buildSubscription(UUID subscriptionId, SubscriptionStatus status) {
        AuditLog auditLog = new AuditLog();
        return BillingSubscription.builder()
                .id(subscriptionId)
                .customerId(UUID.randomUUID())
                .plan(buildPlan())
                .subscriptionStatus(status)
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .auditLog(auditLog)
                .build();
    }

    @Test
    void createSubscription_shouldReturnSavedSubscription_whenDataIsValid() {
        UUID planId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BillingSubscriptionRequestDTO request = new BillingSubscriptionRequestDTO(planId, customerId, "pm_test123");

        Plan plan = buildPlan();
        plan.setId(planId);
        CustomerClientResponse customer = buildCustomer();
        BillingSubscription subscription = buildSubscription(UUID.randomUUID(), SubscriptionStatus.PENDING);
        SubscriptionCreatedEvent event = mock(SubscriptionCreatedEvent.class);

        when(customerApiService.findCustomer(customerId)).thenReturn(customer);
        doNothing().when(stripeCustomerService).ensureCustomerExistsOnStripe(customer.stripeCustomerId());
        when(planService.findPlanById(planId)).thenReturn(plan);
        doNothing().when(subscriptionValidator).validateSubscription(plan.getId(), customer.id());
        doNothing().when(stripePlanService).ensurePlanExistsOnStripe(plan.getStripePriceId());
        when(billingSubscriptionMapper.toEntity(plan, customer.id())).thenReturn(subscription);
        when(billingSubscriptionRepository.save(subscription)).thenReturn(subscription);
        when(billingSubscriptionMapper.mapToSubscriptionCreatedEvent(subscription, customer, "pm_test123")).thenReturn(event);
        doNothing().when(subscriptionEventPublisher).publisherSubscriptionCreated(event);

        BillingSubscription result = billingSubscriptionService.createSubscription(request);

        assertThat(result).isNotNull();
        assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PENDING);
        verify(subscriptionEventPublisher).publisherSubscriptionCreated(event);
    }

    @Test
    void createSubscription_shouldThrowNotFoundException_whenCustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        BillingSubscriptionRequestDTO request = new BillingSubscriptionRequestDTO(planId, customerId, "pm_test");
        when(customerApiService.findCustomer(customerId)).thenThrow(new NotFoundException("Customer not found"));

        assertThatThrownBy(() -> billingSubscriptionService.createSubscription(request))
                .isInstanceOf(NotFoundException.class);

        verify(billingSubscriptionRepository, never()).save(any());
    }

    @Test
    void findSubscriptionById_shouldReturnSubscription_whenIdExists() {
        UUID subscriptionId = UUID.randomUUID();
        BillingSubscription subscription = buildSubscription(subscriptionId, SubscriptionStatus.ACTIVE);

        when(billingSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        BillingSubscription result = billingSubscriptionService.findSubscriptionById(subscriptionId);

        assertThat(result.getId()).isEqualTo(subscriptionId);
    }

    @Test
    void findSubscriptionById_shouldThrowNotFoundException_whenIdDoesNotExist() {
        UUID subscriptionId = UUID.randomUUID();
        when(billingSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingSubscriptionService.findSubscriptionById(subscriptionId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No subscription found with ID");
    }

    @Test
    void activeSubscription_shouldUpdateStatusToActive_whenSubscriptionIsPending() {
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        BillingSubscription subscription = buildSubscription(subscriptionId, SubscriptionStatus.PENDING);

        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .subscriptionId(subscriptionId)
                .paymentId(paymentId)
                .stripeSubscriptionId("sub_stripe123")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .build();

        when(billingSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(billingSubscriptionRepository.save(any(BillingSubscription.class))).thenReturn(subscription);

        billingSubscriptionService.activeSubscription(event);

        assertThat(subscription.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStripeSubscriptionId()).isEqualTo("sub_stripe123");
        verify(billingSubscriptionRepository).save(subscription);
    }

    @Test
    void activeSubscription_shouldSkipUpdate_whenSubscriptionIsAlreadyActive() {
        UUID subscriptionId = UUID.randomUUID();
        BillingSubscription subscription = buildSubscription(subscriptionId, SubscriptionStatus.ACTIVE);

        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .subscriptionId(subscriptionId)
                .paymentId(UUID.randomUUID())
                .stripeSubscriptionId("sub_stripe123")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .build();

        when(billingSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        billingSubscriptionService.activeSubscription(event);

        verify(billingSubscriptionRepository, never()).save(any());
    }

    @Test
    void cancelSubscription_shouldUpdateStatusToCanceled_whenSubscriptionIsActive() {
        UUID subscriptionId = UUID.randomUUID();
        BillingSubscription subscription = buildSubscription(subscriptionId, SubscriptionStatus.ACTIVE);

        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .subscriptionId(subscriptionId)
                .paymentId(UUID.randomUUID())
                .stripeSubscriptionId("sub_stripe123")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .build();

        when(billingSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(billingSubscriptionRepository.save(any(BillingSubscription.class))).thenReturn(subscription);

        billingSubscriptionService.cancelSubscription(event);

        assertThat(subscription.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        verify(billingSubscriptionRepository).save(subscription);
    }

    @Test
    void cancelSubscription_shouldSkipUpdate_whenSubscriptionIsAlreadyCanceled() {
        UUID subscriptionId = UUID.randomUUID();
        BillingSubscription subscription = buildSubscription(subscriptionId, SubscriptionStatus.CANCELED);

        SubscriptionPaymentEvent event = SubscriptionPaymentEvent.builder()
                .subscriptionId(subscriptionId)
                .paymentId(UUID.randomUUID())
                .stripeSubscriptionId("sub_stripe123")
                .currentPeriodStart(LocalDate.now())
                .currentPeriodEnd(LocalDate.now().plusMonths(1))
                .build();

        when(billingSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        billingSubscriptionService.cancelSubscription(event);

        verify(billingSubscriptionRepository, never()).save(any());
    }
}
