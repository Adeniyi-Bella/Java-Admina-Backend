package com.admina.api.notification;

public class RetryableNotificationException extends RuntimeException {
    public RetryableNotificationException(String message) {
        super(message);
    }

    public RetryableNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
