package com.admina.api.exceptions;

import com.admina.api.config.rabbit.RabbitCoreConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class GlobalDlqAlertListener {

    @RabbitListener(queues = RabbitCoreConfig.GLOBAL_DLQ)
    public void onDeadLetter(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error(
                "ALERT: Message moved to DLQ queue={} routingKey={} redelivered={} headers={} payload={}",
                RabbitCoreConfig.GLOBAL_DLQ,
                message.getMessageProperties().getReceivedRoutingKey(),
                message.getMessageProperties().getRedelivered(),
                message.getMessageProperties().getHeaders(),
                payload);
    }
}
