package com.admina.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

import java.util.List;

import com.admina.api.config.AppSecurityProperties;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String issuerUri;
    private final String audience;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
        @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        String issuerUri,
        AppSecurityProperties appSecurityProperties,
        RateLimitFilter rateLimitFilter
    ) {
        this.issuerUri = issuerUri;
        this.audience = appSecurityProperties.audience();
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class)
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);
        if (decoder instanceof org.springframework.security.oauth2.jwt.NimbusJwtDecoder nimbus) {
            nimbus.setJwtValidator(validator);
            return nimbus;
        }
        throw new JwtException("Unsupported JwtDecoder type: " + decoder.getClass().getName());
    }

    private static final class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String audience;

        private AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public org.springframework.security.oauth2.core.OAuth2TokenValidatorResult validate(Jwt token) {
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.contains(audience)) {
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "Invalid audience", null);
            return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(error);
        }
    }
}
