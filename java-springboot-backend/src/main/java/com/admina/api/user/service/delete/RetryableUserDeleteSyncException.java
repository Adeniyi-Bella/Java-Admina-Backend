package com.admina.api.user.service.delete;

public class RetryableUserDeleteSyncException extends RuntimeException {
    public RetryableUserDeleteSyncException(String message) {
        super(message);
    }

    public RetryableUserDeleteSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
