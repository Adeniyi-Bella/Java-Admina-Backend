package com.admina.api.service.notification;

import com.admina.api.model.user.User;

public interface NotificationService {
    void sendWelcomeEmail(User user);
}
