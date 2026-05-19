package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.StripeIntegrationException;
import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.model.enums.IntervalPlan;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.param.PriceCreateParams;
import org.springframework.stereotype.Service;

@Service
public class PlanStripeService {

    public String createStripePrice(PlanRequestDTO request) {
        try {
            PriceCreateParams params = PriceCreateParams.builder()
                    .setCurrency(request.currency().toLowerCase())
                    .setUnitAmount(request.price().longValue())
                    .setRecurring(PriceCreateParams.Recurring.builder().setInterval(
                            request.interval() == IntervalPlan.MONTHLY
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
            throw new StripeIntegrationException("Failed to create price on Stripe: " + e);
        }
    }
}