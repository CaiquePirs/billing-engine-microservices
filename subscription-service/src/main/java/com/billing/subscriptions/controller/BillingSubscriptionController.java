package com.billing.subscriptions.controller;

import com.billing.subscriptions.controller.dto.BillingSubscriptionRequestDTO;
import com.billing.subscriptions.controller.dto.BillingSubscriptionResponseDTO;
import com.billing.subscriptions.mapper.BillingSubscriptionMapper;
import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.service.BillingSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class BillingSubscriptionController {

    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingSubscriptionMapper billingSubscriptionMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<BillingSubscriptionResponseDTO> createSubscription(@RequestBody @Valid BillingSubscriptionRequestDTO billingSubscriptionRequestDTO) {
        BillingSubscription billingSubscription = billingSubscriptionService.createSubscription(billingSubscriptionRequestDTO);
        BillingSubscriptionResponseDTO billingResponse = billingSubscriptionMapper.toResponse(billingSubscription);
        return ResponseEntity.status(HttpStatus.CREATED).body(billingResponse);
    }
}
