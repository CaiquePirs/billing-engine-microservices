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

import java.math.RoundingMode;

@Slf4j
@Service
public class StripePlanService {

    public String createStripePrice(PlanRequestDTO request) {
        try {
            PriceCreateParams params = PriceCreateParams.builder()
                    .setCurrency(request.currency().toLowerCase())
                    .setUnitAmount(request.price()
                            .movePointRight(2)
                            .setScale(0, RoundingMode.UNNECESSARY)
                            .longValueExact())
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
            log.info("Plan created on Stripe (stripePriceId={}, planName={})", price.getId(), request.name());

            return price.getId();

        } catch (StripeException e) {
            log.error("Failed to create Stripe price for plan '{}' with currency '{}'", request.name(), request.currency(), e);
            throw new StripeIntegrationException("Failed to create Stripe price for plan '" + request.name() + "'. Check plan configuration and try again.");
        }
    }

    public void ensurePlanExistsOnStripe(String planId) {
        try {
            Plan plan = Plan.retrieve(planId);
            if(plan == null) {
                throw new StripeIntegrationException("Stripe plan not found for ID: " + planId);
            }

        } catch (StripeException e) {
            log.error("Stripe API error while verifying plan with ID {}", planId, e);
            throw new StripeIntegrationException("Failed to verify Stripe plan with ID: " + planId + ". Subscription creation aborted.");
        }
    }

}