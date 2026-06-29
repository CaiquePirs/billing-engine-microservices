package com.billing.payment.controller;

import com.billing.payment.config.SecurityConfig;
import com.billing.payment.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StripeWebhookController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "eureka.client.enabled=false",
        "STRIPE_WEBHOOK_SIGN=whsec_test_dummy_secret"
})
class StripeWebhookControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PaymentService paymentService;

    private static final String BASE_URL = "/api/v1/webhooks/stripe";
    private static final String PAYLOAD = "{\"id\":\"evt_test\",\"type\":\"invoice.paid\"}";

    @Test
    void processWebhook_shouldReturn400_whenStripeSignatureHeaderIsMissing() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).handlerPaymentEvent(any(), anyString());
    }

    @Test
    void processWebhook_shouldReturn400_whenStripeSignatureIsInvalid() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD)
                        .header("Stripe-Signature", "t=invalid,v1=badsig"))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).handlerPaymentEvent(any(), anyString());
    }

    @Test
    void processWebhook_shouldReturn200_whenWebhookIsProcessedSuccessfully() throws Exception {
        Event stripeEvent = mock(Event.class);

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(stripeEvent);
            doNothing().when(paymentService).handlerPaymentEvent(eq(stripeEvent), anyString());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PAYLOAD)
                            .header("Stripe-Signature", "t=1234567890,v1=valid_signature"))
                    .andExpect(status().isOk());

            verify(paymentService).handlerPaymentEvent(eq(stripeEvent), anyString());
        }
    }
}
