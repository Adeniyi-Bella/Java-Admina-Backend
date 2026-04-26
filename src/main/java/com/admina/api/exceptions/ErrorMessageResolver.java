package com.admina.api.exceptions;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ErrorMessageResolver {

    private final Environment environment;

    public ErrorMessageResolver(Environment environment) {
        this.environment = environment;
    }

    public String resolve(String providedMessage, String defaultMessage) {
        if (!isDev()) {
            return defaultMessage;
        }
        if (StringUtils.hasText(providedMessage)) {
            return providedMessage;
        }
        return defaultMessage;
    }

    private boolean isDev() {
        return environment.matchesProfiles("dev");
    }
}
