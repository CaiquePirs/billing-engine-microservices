package com.billing.subscriptions.service;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
import com.billing.subscriptions.events.publisher.SubscriptionEventPublisher;
import com.billing.subscriptions.mapper.BillingSubscriptionMapper;
import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.repository.BillingSubscriptionRepository;
import com.stripe.model.Subscription;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
