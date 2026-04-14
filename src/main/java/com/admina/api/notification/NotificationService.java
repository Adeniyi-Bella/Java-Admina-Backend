package com.admina.api.notification;

import com.admina.api.user.model.User;

public interface NotificationService {
    void sendWelcomeEmail(User user);
}
