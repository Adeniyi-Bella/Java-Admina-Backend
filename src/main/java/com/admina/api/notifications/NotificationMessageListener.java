package com.admina.api.notifications;

import com.admina.api.config.RabbitConfig;
import com.admina.api.model.User;
import com.admina.api.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationMessageListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onWelcomeMessage(NotificationMessage message) {
        User user = User.builder()
            .id(message.userId())
            .email(message.email())
            .username(message.username())
            .build();
        try {
            notificationService.sendWelcomeEmail(user);
        } catch (com.admina.api.service.notification.RetryableNotificationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Non-retryable notification failure userId={}", message.userId(), ex);
        }
    }
}
