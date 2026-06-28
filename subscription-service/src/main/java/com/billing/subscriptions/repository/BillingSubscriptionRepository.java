package com.billing.subscriptions.repository;

import com.billing.subscriptions.model.BillingSubscription;
import com.billing.subscriptions.model.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscription, UUID> {

    boolean existsByCustomerIdAndPlan_IdAndSubscriptionStatusIn(UUID customerId, UUID planId, List<SubscriptionStatus> statuses);
}
