package com.admina.api.security;

import com.admina.api.exceptions.AppExceptions;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PrincipalValidator {

    private final Validator validator;

    public void validate(AuthenticatedPrincipal principal) {
        Set<ConstraintViolation<AuthenticatedPrincipal>> violations = validator.validate(principal);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
            throw new AppExceptions.UnauthorizedException("Invalid token claims: " + message);
        }
    }
}
