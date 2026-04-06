package com.admina.api.config;

import com.admina.api.config.properties.AppStripeProperties;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StripeConfig {

    private final AppStripeProperties stripeProperties;

    @PostConstruct
    public void configureStripe() {
        Stripe.apiKey = stripeProperties.secretKey();
    }
}