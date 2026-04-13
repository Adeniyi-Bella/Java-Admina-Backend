package com.admina.api.dto.payment;

import com.admina.api.enums.PlanType;
import com.admina.api.exceptions.AppExceptions;

import jakarta.validation.constraints.NotNull;

public record SubscriptionCheckoutRequest(

                @NotNull(message = "Plan is required") String plan

) {
        public PlanType planType() {

                if (plan == null) {
                        throw new AppExceptions.BadRequestException("Plan is required");
                }

                try {
                        return PlanType.valueOf(plan.toUpperCase());
                } catch (IllegalArgumentException e) {
                        throw new AppExceptions.BadRequestException("Invalid plan: " + plan);
                }
        }
}