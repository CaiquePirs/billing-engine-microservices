package com.billing.payment.mapper;

import com.billing.payment.events.data.*;
import com.billing.payment.model.AuditLog;
import com.billing.payment.model.Payment;
import com.billing.payment.model.enums.PaymentStatus;
import com.billing.payment.service.StripeCustomerService;
import com.billing.payment.service.StripePlanService;
import com.billing.payment.utils.PaymentUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private final PaymentUtils paymentUtils;
    private final StripePlanService stripePlanService;
    private final StripeCustomerService stripeCustomerService;

    public Payment toEntity(SubscriptionCreatedEvent subscriptionEvent) {
        return Payment.builder()
                .subscriptionId(subscriptionEvent.id())
                .customerId(subscriptionEvent.customer().id())
                .amount(subscriptionEvent.plan().price().longValue())
                .currency(subscriptionEvent.plan().currency())
                .paymentStatus(PaymentStatus.PENDING)
                .auditLog(new AuditLog())
                .build();
    }

    public SubscriptionPaymentEvent toEvent(Payment payment, Event event) {
        JsonNode root = paymentUtils.getEventRoot(event);

        String customerStripeId = paymentUtils.getTextOrNull(root, "customer");
        String stripePriceId = paymentUtils.extractPlanId(event);

        Customer customer = stripeCustomerService.findCustomerOnStripeById(customerStripeId);
        Price price = stripePlanService.findPriceById(stripePriceId);
        Product product = stripePlanService.findProductByPriceId(stripePriceId);

        return SubscriptionPaymentEvent.builder()
                .paymentId(payment.getId())
                .subscriptionId(payment.getSubscriptionId())
                .stripeSubscriptionId(paymentUtils.getStripeSubscriptionId(event))
                .currentPeriodStart(paymentUtils.getCurrentPeriodStart(event))
                .currentPeriodEnd(paymentUtils.getCurrentPeriodEnd(event))
                .paymentStatus(PaymentStatus.APPROVED.toString())
                .subscriptionStatus("ACTIVE")
                .plan(mapToPlan(price, product))
                .customer(mapToCustomer(customer))
                .build();
    }

    private CustomerClientResponse mapToCustomer(Customer customer){
        return CustomerClientResponse.builder()
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(CustomerAddressResponse.builder()
                        .city(customer.getAddress().getCity())
                        .state(customer.getAddress().getState())
                        .eircode(customer.getAddress().getPostalCode())
                        .county(customer.getAddress().getCountry())
                        .number(customer.getAddress().getLine2())
                        .street(customer.getAddress().getLine1())
                        .build())
                .build();
    }

    private PlanResponseDTO mapToPlan(Price price, Product product){
       return PlanResponseDTO.builder()
                .name(product.getName())
               .description(product.getDescription())
                .price(price.getUnitAmountDecimal())
                .active(price.getActive())
                .interval(price.getRecurring().getInterval())
                .currency(price.getCurrency().toUpperCase())
                .build();
    }
}
