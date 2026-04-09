package com.admina.api.service.user.delete;

public class RetryableUserDeleteSyncException extends RuntimeException {
    public RetryableUserDeleteSyncException(String message) {
        super(message);
    }

    public RetryableUserDeleteSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
