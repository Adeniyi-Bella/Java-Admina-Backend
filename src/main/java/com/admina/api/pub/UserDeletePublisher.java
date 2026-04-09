package com.admina.api.pub;

import com.admina.api.config.rabbit.UserDeleteRabbitConfig;
import com.admina.api.events.user.UserDeletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeletePublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeletedFromDb(UserDeletedEvent event) {
        publishDisableUser(event);
    }

    public void publishDisableUser(UserDeletedEvent message) {
        rabbitTemplate.convertAndSend(
                UserDeleteRabbitConfig.USER_DELETE_EXCHANGE,
                UserDeleteRabbitConfig.USER_DELETE_ROUTING_KEY,
                message);
        log.info("Queued Entra user disable userOid={} email={}", message.userOid(), message.email());
    }
}
