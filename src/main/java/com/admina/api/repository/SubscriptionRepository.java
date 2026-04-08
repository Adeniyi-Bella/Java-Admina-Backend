package com.admina.api.repository;

import com.admina.api.model.subscription.UserStripeSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<UserStripeSubscription, UUID> {
    Optional<UserStripeSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}