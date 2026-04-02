package com.admina.api.config.properties;

import com.admina.api.enums.PlanType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.plans")
public record AppPlanProperties(
        Map<String, PlanLimit> limits) {
    public record PlanLimit(int max) {
    }

    public int getMaxForPlan(PlanType plan) {
        PlanLimit limit = limits.get(plan.name().toLowerCase());
        if (limit == null) {
            throw new IllegalStateException("No plan limit configured for plan: " + plan);
        }
        return limit.max();
    }
}
