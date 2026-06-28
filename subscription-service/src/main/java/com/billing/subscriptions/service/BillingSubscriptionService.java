package com.billing.subscriptions.service;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.advice.exception.NotFoundException;
import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
import com.billing.subscriptions.events.data.SubscriptionCreatedEvent;
import com.billing.subscriptions.events.data.SubscriptionPaymentEvent;
import com.billing.subscriptions.events.publisher.SubscriptionEventPublisher;
import com.billing.subscriptions.mapper.BillingSubscriptionMapper;
import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import com.billing.subscriptions.repository.BillingSubscriptionRepository;
import com.billing.subscriptions.validator.SubscriptionValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingSubscriptionService {

    private final PlanService planService;
    private final CustomerApiService customerApiService;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final BillingSubscriptionMapper billingSubscriptionMapper;
    private final StripeCustomerService stripeCustomerService;
    private final StripePlanService stripePlanService;
    private final SubscriptionEventPublisher subscriptionEventPublisher;
    private final SubscriptionValidator subscriptionValidator;

    @Transactional
    public BillingSubscription createSubscription(BillingSubscriptionRequestDTO subscriptionRequest) {
        CustomerClientResponse customer = customerApiService.findCustomer(subscriptionRequest.customerId());
        stripeCustomerService.ensureCustomerExistsOnStripe(customer.stripeCustomerId());

        Plan plan = planService.findPlanById(subscriptionRequest.planId());

        subscriptionValidator.validateSubscription(plan.getId(), customer.id());
        stripePlanService.ensurePlanExistsOnStripe(plan.getStripePriceId());

        BillingSubscription subscription = billingSubscriptionMapper.toEntity(plan, customer.id());
        BillingSubscription subscriptionCreated = billingSubscriptionRepository.save(subscription);

        SubscriptionCreatedEvent subscriptionEventMessage = billingSubscriptionMapper.mapToSubscriptionCreatedEvent(
                subscriptionCreated,
                customer,
                subscriptionRequest.paymentMethodId()
        );

        subscriptionEventPublisher.publisherSubscriptionCreated(subscriptionEventMessage);
        return subscriptionCreated;
    }

    public BillingSubscription findSubscriptionById(UUID subscriptionId) {
        return billingSubscriptionRepository
                .findById(subscriptionId)
                .orElseThrow(() -> {
                    log.error("Subscription lookup failed: no subscription found with ID: {}", subscriptionId);
                    return new NotFoundException("No subscription found with ID: " + subscriptionId);
                });
    }

    public void activeSubscription(SubscriptionPaymentEvent event) {
        BillingSubscription subscription = findSubscriptionById(event.subscriptionId());

        if(!subscription.getSubscriptionStatus().equals(SubscriptionStatus.ACTIVE)) {

            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            subscription.setStripeSubscriptionId(event.stripeSubscriptionId());
            subscription.setCurrentPeriodStart(event.currentPeriodStart());
            subscription.setCurrentPeriodEnd(event.currentPeriodEnd());
            subscription.getAuditLog().setUpdatedAt(LocalDateTime.now());

            billingSubscriptionRepository.save(subscription);
        }else {
            log.info("Subscription ID: {} is already active, skipping re-activation", event.subscriptionId());
            return;
        }
    }

    public void cancelSubscription(SubscriptionPaymentEvent event) {
        BillingSubscription subscription = findSubscriptionById(event.subscriptionId());

        if(!subscription.getSubscriptionStatus().equals(SubscriptionStatus.CANCELED)) {

            subscription.setSubscriptionStatus(SubscriptionStatus.CANCELED);
            subscription.setStripeSubscriptionId(event.stripeSubscriptionId());
            subscription.setCurrentPeriodStart(event.currentPeriodStart());
            subscription.setCurrentPeriodEnd(event.currentPeriodEnd());
            subscription.getAuditLog().setUpdatedAt(LocalDateTime.now());

            billingSubscriptionRepository.save(subscription);
        }else {
            log.info("Subscription ID: {} is already cancelled, skipping cancellation", event.subscriptionId());
            return;
        }
    }

}
