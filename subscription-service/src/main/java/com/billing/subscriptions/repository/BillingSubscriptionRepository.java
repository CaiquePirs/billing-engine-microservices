package com.billing.subscriptions.repository;

import com.billing.subscriptions.model.BillingSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscription, UUID> {
}
