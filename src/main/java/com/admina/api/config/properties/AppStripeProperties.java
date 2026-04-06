
package com.admina.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.stripe")
public record AppStripeProperties(
        String secretKey,
        String webhookSecret,
        Map<String, String> priceIds) {

    public String getPriceIdForPlan(String plan) {
        String key = plan.toLowerCase();
        if (!priceIds.containsKey(key)) {
            throw new IllegalArgumentException("Unknown Stripe plan: " + plan + ". Configured: " + priceIds.keySet());
        }
        return priceIds.get(key);
    }
}

