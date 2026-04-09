package com.admina.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.admina.api.model.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.planLimitCurrent = u.planLimitCurrent - 1 WHERE u.id = :userId AND u.planLimitCurrent > 0")
    int decrementPlanLimitIfPositive(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM User u WHERE u.email = :email")
    int deleteByEmail(@Param("email") String email);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);
}
