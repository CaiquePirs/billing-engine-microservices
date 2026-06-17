package com.billing.payment.service;

import com.billing.payment.controller.advice.exceptions.StripeIntegrationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePlanService {

    public Price findPriceById(String priceId) {
        try {
            return Price.retrieve(priceId);

        } catch (StripeException e) {
            log.error("Failed to fetch Stripe price by ID {}", priceId, e);
            throw new StripeIntegrationException("Failed to fetch Stripe price.");
        }
    }

    public Product findProductByPriceId(String priceId) {
        try {
            Price price = findPriceById(priceId);
            return Product.retrieve(price.getProduct());

        } catch (StripeException e) {
            log.error("Failed to fetch Stripe product by price ID {}", priceId, e);
            throw new StripeIntegrationException("Failed to fetch Stripe product.");
        }
    }


}
