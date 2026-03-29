package com.admina.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AppExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public static class TooManyRequestsException extends RuntimeException {
        public TooManyRequestsException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class InternalServerErrorException extends RuntimeException {
        public InternalServerErrorException(String message) {
            super(message);
        }
    }
}
