package com.billing.subscriptions.client.api;

import com.billing.subscriptions.client.dto.CustomerClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "customers-service")
public interface CustomerClientApi {

    @GetMapping("/api/v1/customers/{id}")
    ResponseEntity<CustomerClientResponse> findCustomerById(@PathVariable(name = "id") UUID customerId);

}
