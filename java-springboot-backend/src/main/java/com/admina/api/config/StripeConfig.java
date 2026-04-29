package com.admina.api.config;

import com.admina.api.config.properties.AppStripeProperties;
import com.stripe.StripeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(AppStripeProperties stripeProperties) {
        return new StripeClient(stripeProperties.secretKey());
    }
}
