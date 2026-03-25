package com.admina.api.service.document;

import com.admina.api.config.RabbitConfig;
import com.admina.api.dto.document.DocumentJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentJobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(DocumentJobMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.DOC_EXCHANGE, RabbitConfig.DOC_ROUTING_KEY, message);
        log.info("Queued document job docId={}", message.docId());
    }
}
