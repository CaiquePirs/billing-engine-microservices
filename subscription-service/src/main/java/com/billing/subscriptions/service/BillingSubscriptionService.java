package com.billing.subscriptions.service;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
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
    private final StripePlanService stripePlanService;
    private final CustomerApiService customerApiService;
    private final StripeCustomerService stripeCustomerService;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final StripeSubscriptionService stripeSubscriptionService;
    private final BillingSubscriptionMapper billingSubscriptionMapper;

    @Transactional
    public BillingSubscription createSubscription(BillingSubscriptionRequestDTO subscriptionRequest) {
        CustomerClientResponse customer = customerApiService.findCustomer(subscriptionRequest.customerId());
        stripeCustomerService.ensureCustomerExistsOnStripe(customer.stripeCustomerId());

        Plan plan = planService.findPlanById(subscriptionRequest.planId());
        stripePlanService.ensurePlanExistsOnStripe(plan.getStripePriceId());

        Subscription stripeSubscription = stripeSubscriptionService.createSubscription(
                customer.stripeCustomerId(),
                plan.getStripePriceId()
        );

        BillingSubscription subscription = billingSubscriptionMapper.toEntity(
                stripeSubscription,
                plan,
                customer.id()
        );

        return billingSubscriptionRepository.save(subscription);
    }
}
