package com.admina.api.service.notification;

import com.admina.api.user.model.User;

public interface NotificationService {
    void sendWelcomeEmail(User user);
}
