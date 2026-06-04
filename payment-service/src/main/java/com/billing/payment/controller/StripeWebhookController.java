package com.billing.payment.controller;

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

        } catch (Exception e){
            log.error("Error processing stripe webhook event", e);
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
