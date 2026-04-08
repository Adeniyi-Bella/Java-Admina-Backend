package com.admina.api.pub;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.admina.api.config.rabbit.WebhookRabbitConfig;
import com.admina.api.events.subscription.SubscriptionUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(SubscriptionUpdatedEvent message) {
        rabbitTemplate.convertAndSend(WebhookRabbitConfig.SUBSCRIPTION_EXCHANGE, WebhookRabbitConfig.SUBSCRIPTION_ROUTING_KEY,
                message);
        log.info("Published subscription update event for userId: {}", message.userId());
    }
}
