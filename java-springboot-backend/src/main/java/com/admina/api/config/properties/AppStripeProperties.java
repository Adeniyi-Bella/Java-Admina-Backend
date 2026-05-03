
package com.admina.api.config.properties;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.admina.api.user.enums.PlanType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app.stripe")
@Validated
public record AppStripeProperties(
                @NotBlank(message = "Stripe secret key is required") String secretKey,

                @NotBlank(message = "Stripe webhook secret is required") String webhookSecret,

                @NotBlank(message = "Stripe checkout URL is required") String checkoutUrl,

                @NotBlank(message = "Stripe portal return URL is required") String portalReturnUrl,

                @NotNull @NotEmpty Map<String, String> priceIds) {

        public AppStripeProperties {
                for (PlanType plan : PlanType.values()) {
                        if (plan == PlanType.FREE)
                                continue;
                        if (!priceIds.containsKey(plan.name())) {
                                throw new IllegalStateException(
                                                "Missing Stripe priceId for plan: " + plan.name() +
                                                                ". Check app.stripe.price-ids in application.yml");
                        }
                }
        }

        public String getPriceIdForPlan(String plan) {
                return priceIds.get(plan);
        }
}
