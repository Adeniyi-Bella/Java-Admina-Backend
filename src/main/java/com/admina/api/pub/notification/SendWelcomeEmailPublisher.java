package com.admina.api.pub.notification;

import com.admina.api.config.RabbitConfig;
import com.admina.api.events.notification.SendWelcomeEmailEvent;

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
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, message);
        log.info("Queued welcome email userId={}", message.userId());
    }
}
