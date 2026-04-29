package com.admina.api.document.pub;

import com.admina.api.config.rabbit.ChatRabbitConfig;
import com.admina.api.document.events.ChatJobEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatJobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(ChatJobEvent message) {
        rabbitTemplate.convertAndSend(ChatRabbitConfig.CHAT_EXCHANGE, ChatRabbitConfig.CHAT_ROUTING_KEY, message);
        log.info("Queued chat job chatbotPollingId={} docId={}", message.chatbotPollingId(), message.docId());
    }
}
