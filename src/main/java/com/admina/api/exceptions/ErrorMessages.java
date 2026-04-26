package com.admina.api.exceptions;

import org.springframework.http.HttpStatus;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static String defaultMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Bad request";
            case UNAUTHORIZED -> "Authentication failed";
            case PAYMENT_REQUIRED -> "Payment required";
            case FORBIDDEN -> "Access denied";
            case NOT_FOUND -> "Resource not found";
            case METHOD_NOT_ALLOWED -> "Method not allowed";
            case CONFLICT -> "Conflict occurred";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported media type";
            case TOO_MANY_REQUESTS -> "Too many requests";
            case INTERNAL_SERVER_ERROR -> "Internal server error";
            case BAD_GATEWAY -> "Bad gateway";
            case SERVICE_UNAVAILABLE -> "Service unavailable";
            case GATEWAY_TIMEOUT -> "Gateway timeout";
            default -> "Internal server error";
        };
    }
}
