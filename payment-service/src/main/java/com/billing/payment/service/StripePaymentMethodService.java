package com.billing.payment.service;

import com.billing.payment.controller.advice.StripeIntegrationException;
import com.billing.payment.events.data.SubscriptionCreatedEvent;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentMethodService {

    /*
            In a real-world scenario, the payment method is created and attached
            to the customer directly via Stripe.js on the frontend.
            The backend only receives the `paymentMethodId`.

            Since this project has no frontend, the `attachPaymentMethod` method
            simulates this process server-side for testing purposes only.
    * */

    @CircuitBreaker(name = "stripe")
    @Retry(name = "stripe")
    public String attachPaymentMethod(SubscriptionCreatedEvent event) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(event.paymentMethodId());

            PaymentMethod attachedPaymentMethod = paymentMethod.attach(
                    PaymentMethodAttachParams.builder()
                            .setCustomer(event.customer().stripeCustomerId())
                            .build()
            );

            Customer customer = Customer.retrieve(event.customer().stripeCustomerId());
            customer.update(
                    CustomerUpdateParams.builder()
                            .setInvoiceSettings(
                                    CustomerUpdateParams.InvoiceSettings.builder()
                                            .setDefaultPaymentMethod(attachedPaymentMethod.getId())
                                            .build()
                            )
                            .build()
            );

            return attachedPaymentMethod.getId();

        } catch (StripeException e) {
            log.error("Failed to attach paymentMethod to customer {}", event.customer().stripeCustomerId(), e);
            throw new StripeIntegrationException("Processing payment failed. Error with Stripe: " + e.getMessage());
        }
    }
}
