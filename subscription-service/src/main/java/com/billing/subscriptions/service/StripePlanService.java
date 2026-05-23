package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.StripeIntegrationException;
import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.model.enums.IntervalPlan;
import com.stripe.exception.StripeException;
import com.stripe.model.Plan;
import com.stripe.model.Price;
import com.stripe.param.PriceCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StripePlanService {

    public String createStripePrice(PlanRequestDTO request) {
        try {
            PriceCreateParams params = PriceCreateParams.builder()
                    .setCurrency(request.currency().toLowerCase())
                    .setUnitAmount(request.price().longValue())
                    .setRecurring(PriceCreateParams.Recurring.builder()
                            .setInterval(request.interval() == IntervalPlan.MONTHLY
                                    ? PriceCreateParams.Recurring.Interval.MONTH
                                    : PriceCreateParams.Recurring.Interval.YEAR
                                    )
                            .build()
                    )
                    .setProductData(PriceCreateParams.ProductData.builder()
                                    .setName(request.name())
                                    .build())
                    .build();

            Price price = Price.create(params);
            return price.getId();

        } catch (StripeException e) {
            log.error("Failed to create Stripe Price", e);
            throw new StripeIntegrationException("Failed to create a plan. Try again later");
        }
    }

    public void ensurePlanExistsOnStripe(String planId) {
        try {
            Plan plan = Plan.retrieve(planId);
            if(plan == null) {
                throw new StripeIntegrationException("Plan not found");
            }

        } catch (StripeException e) {
            log.error("Failed to fetching plan by ID {}", planId, e);
            throw new StripeIntegrationException("Subscription failed. Try again later.");
        }
    }

}