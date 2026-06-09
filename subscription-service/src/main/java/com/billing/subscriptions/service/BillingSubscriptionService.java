package com.billing.subscriptions.service;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.advice.exception.NotFoundException;
import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
import com.billing.subscriptions.events.data.SubscriptionPaymentEvent;
import com.billing.subscriptions.events.publisher.SubscriptionEventPublisher;
import com.billing.subscriptions.mapper.BillingSubscriptionMapper;
import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import com.billing.subscriptions.repository.BillingSubscriptionRepository;
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
public class BillingSubscriptionService {

    private final PlanService planService;
    private final CustomerApiService customerApiService;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final BillingSubscriptionMapper billingSubscriptionMapper;
    private final StripeCustomerService stripeCustomerService;
    private final StripePlanService stripePlanService;
    private final SubscriptionEventPublisher subscriptionEventPublisher;

    @Transactional
    public BillingSubscription createSubscription(BillingSubscriptionRequestDTO subscriptionRequest) {
        CustomerClientResponse customer = customerApiService.findCustomer(subscriptionRequest.customerId());
        stripeCustomerService.ensureCustomerExistsOnStripe(customer.stripeCustomerId());

        Plan plan = planService.findPlanById(subscriptionRequest.planId());
        stripePlanService.ensurePlanExistsOnStripe(plan.getStripePriceId());

        BillingSubscription subscription = billingSubscriptionMapper.toEntity(plan, customer.id());
        BillingSubscription subscriptionCreated = billingSubscriptionRepository.save(subscription);

        subscriptionEventPublisher.publisherSubscriptionCreated(
                subscriptionCreated,
                customer,
                subscriptionRequest.paymentMethodId()
        );

        return subscriptionCreated;
    }

    public BillingSubscription findSubscriptionById(UUID subscriptionId) {
        return billingSubscriptionRepository
                .findById(subscriptionId)
                .orElseThrow(() -> {
                    log.error("Could not active subscription with ID: {} it was not found", subscriptionId);
                    return new NotFoundException("Subscription ID not found");
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
            log.info("Subscription is already active, skipping activation");
            return;
        }
    }

    public void cancelSubscription(SubscriptionPaymentEvent event) {
        BillingSubscription subscription = findSubscriptionById(event.subscriptionId());

        if(!subscription.getSubscriptionStatus().equals(SubscriptionStatus.CANCELLED)) {

            subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
            subscription.setStripeSubscriptionId(event.stripeSubscriptionId());
            subscription.setCurrentPeriodStart(event.currentPeriodStart());
            subscription.setCurrentPeriodEnd(event.currentPeriodEnd());
            subscription.getAuditLog().setUpdatedAt(LocalDateTime.now());

            billingSubscriptionRepository.save(subscription);
        }else {
            log.info("Subscription status is already cancelled, skipping cancellation: {}", subscription.getSubscriptionStatus());
            return;
        }
    }

}
