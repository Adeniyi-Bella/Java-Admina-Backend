package com.admina.api.user.sub;

import com.admina.api.config.rabbit.NotificationRabbitConfig;
import com.admina.api.events.notification.SendWelcomeEmailEvent;
import com.admina.api.redis.RedisKeys;
import com.admina.api.redis.RedisService;
import com.admina.api.service.notification.NotificationService;
import com.admina.api.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendWelcomeEmailListener {

    private static final Duration WELCOME_EMAIL_SENT_TTL = Duration.ofDays(7);
    private static final Duration WELCOME_EMAIL_PENDING_TTL = Duration.ofMinutes(10);

    private final NotificationService notificationService;
    private final RedisService redisService;

    @RabbitListener(queues = NotificationRabbitConfig.NOTIFICATION_QUEUE, containerFactory = "notificationListenerContainerFactory")
    public void onWelcomeMessage(SendWelcomeEmailEvent message) {
        String sentKey = RedisKeys.welcomeSent(message.userId());
        if (redisService.hasKey(sentKey)) {
            log.info("Skipping duplicate welcome email event userId={}", message.userId());
            return;
        }

        String pendingKey = RedisKeys.welcomePending(message.userId());
        if (!redisService.tryAcquireIdempotencyKey(pendingKey, WELCOME_EMAIL_PENDING_TTL)) {
            log.info("Welcome email already being processed userId={}", message.userId());
            return;
        }

        User user = User.builder()
                .id(message.userId())
                .email(message.email())
                .username(message.username())
                .build();
        try {
            notificationService.sendWelcomeEmail(user);
            redisService.setKeyWithTtl(sentKey, "1", WELCOME_EMAIL_SENT_TTL);
            redisService.deleteKey(pendingKey);
        } catch (com.admina.api.service.notification.RetryableNotificationException ex) {
            redisService.deleteKey(pendingKey);
            throw ex;
        } catch (RuntimeException ex) {
            redisService.deleteKey(pendingKey);
            log.warn("Non-retryable notification failure userId={}", message.userId(), ex);
        }
    }
}
