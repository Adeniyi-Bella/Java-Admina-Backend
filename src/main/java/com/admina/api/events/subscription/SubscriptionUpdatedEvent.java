package com.admina.api.events.subscription;

import com.admina.api.enums.PlanType;
import java.util.UUID;

public record SubscriptionUpdatedEvent(
        UUID userId,
        String email,
        PlanType plan,
        String stripeSubscriptionId
) {}