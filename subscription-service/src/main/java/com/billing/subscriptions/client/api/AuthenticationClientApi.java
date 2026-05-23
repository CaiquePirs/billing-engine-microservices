package com.billing.subscriptions.client.api;

import com.billing.subscriptions.client.dto.LoginClientRequest;
import com.billing.subscriptions.client.dto.LoginClientResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "authentication-service")
public interface AuthenticationClientApi {

    @PostMapping("/api/v1/auth/internal-login")
    ResponseEntity<LoginClientResponse> signInInternalService(@RequestBody @Valid LoginClientRequest internalLoginClientRequest);

}
