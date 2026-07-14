package com.billing.subscriptions.service;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.advice.exception.NotFoundException;
import com.billing.subscriptions.controller.advice.exception.UserUnauthorizedException;
import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
import com.billing.subscriptions.events.data.SubscriptionCreatedEvent;
import com.billing.subscriptions.events.data.SubscriptionPaymentEvent;
import com.billing.subscriptions.events.publisher.SubscriptionEventPublisher;
import com.billing.subscriptions.mapper.BillingSubscriptionMapper;
import com.billing.subscriptions.metrics.BillingSubscriptionMetrics;
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
    private final BillingSubscriptionMetrics billingSubscriptionMetrics;
    private final SecurityService securityService;

    @Transactional
    public void createSubscription(BillingSubscriptionRequestDTO subscriptionRequest) {
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

        billingSubscriptionMetrics.recordSubscriptionCreatedTotal();
        log.info("Subscription created successfully (subscriptionId={}, customerId={}, status={})",
                subscriptionCreated.getId(),
                customer.id(),
                subscriptionCreated.getSubscriptionStatus());
    }

    public BillingSubscription findSubscriptionById(UUID subscriptionId) {
        return billingSubscriptionRepository
                .findById(subscriptionId)
                .orElseThrow(() -> {
                    log.warn("Subscription lookup failed: no subscription found (subscriptionId={})", subscriptionId);
                    return new NotFoundException("No subscription found with ID: " + subscriptionId);
                });
    }

    public BillingSubscription findOwnedSubscription(UUID subscriptionId) {
        UUID authenticatedCustomerId = securityService.getLoggedInAdmin();
        BillingSubscription subscription = findSubscriptionById(subscriptionId);

        if (!subscription.getCustomerId().equals(authenticatedCustomerId)) {
            log.warn("Subscription access denied: subscription does not belong to the authenticated customer (subscriptionId={}, customerId={})",
                    subscriptionId, authenticatedCustomerId);
            throw new UserUnauthorizedException("You are not allowed to access this subscription");
        }
        return subscription;
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

            billingSubscriptionMetrics.recordSubscriptionActivatedTotal();
            log.info("Subscription activated (subscriptionId={}, customerId={})", subscription.getId(), subscription.getCustomerId());

        }else {
            log.info("Subscription already active, skipping re-activation (subscriptionId={})", event.subscriptionId());
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

            billingSubscriptionMetrics.recordSubscriptionCancelledTotal();
            log.info("Subscription cancelled (subscriptionId={}, customerId={})", subscription.getId(), subscription.getCustomerId());

        }else {
            log.info("Subscription already cancelled, skipping cancellation (subscriptionId={})", event.subscriptionId());
            return;
        }
    }

}
