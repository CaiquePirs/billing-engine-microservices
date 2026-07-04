package com.billing.payment.controller;

import com.billing.payment.mapper.PaymentMapper;
import com.billing.payment.metrics.PaymentMetrics;
import com.billing.payment.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final PaymentMetrics paymentMetrics;

    @Value("${STRIPE_WEBHOOK_SIGN}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> processWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature){
        try {
            Event event = Webhook.constructEvent(
                    payload,
                    signature,
                    webhookSecret
            );

            paymentService.handlerPaymentEvent(event, payload);
            paymentMetrics.recordStripeWebhookRequestHandledTotal();


        } catch (Exception e){
            log.error("Failed to construct or process Stripe webhook event. Signature validation or event parsing may have failed.", e);
            paymentMetrics.recordStripeWebhookRequestHandlingFailedTotal();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
