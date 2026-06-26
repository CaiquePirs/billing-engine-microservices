package com.billing.notification.model;

import com.billing.notification.events.data.CustomerClientResponse;
import com.billing.notification.events.data.PlanResponseDTO;

import java.time.LocalDate;
import java.util.UUID;

public interface SubscriptionNotificationEvent {
    UUID subscriptionId();
    String subscriptionStatus();
    CustomerClientResponse customer();
    PlanResponseDTO plan();
    LocalDate currentPeriodStart();
    LocalDate currentPeriodEnd();
}
