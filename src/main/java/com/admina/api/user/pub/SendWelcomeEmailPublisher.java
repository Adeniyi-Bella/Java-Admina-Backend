package com.admina.api.user.pub;

import com.admina.api.config.rabbit.NotificationRabbitConfig;
import com.admina.api.notification.SendWelcomeEmailEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendWelcomeEmailPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishWelcome(SendWelcomeEmailEvent message) {
        rabbitTemplate.convertAndSend(NotificationRabbitConfig.NOTIFICATION_EXCHANGE, NotificationRabbitConfig.NOTIFICATION_ROUTING_KEY,
                message);
        log.info("Queued welcome email userId={}", message.userId());
    }
}
