package com.billing.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/api/v1/customers/**").uri("lb://customers-service"))
                .route(r -> r.path("/api/v1/auth/**").uri("lb://authentication-service"))
                .route(r -> r.path("/api/v1/plans/**").uri("lb://subscription-service"))
                .build();
    }
}
