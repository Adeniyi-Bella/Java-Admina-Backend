package com.admina.api.user.sub;

import com.admina.api.config.rabbit.UserDeleteRabbitConfig;
import com.admina.api.user.events.UserDeletedEvent;
import com.admina.api.user.service.delete.RetryableUserDeleteSyncException;
import com.admina.api.user.service.delete.UserDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeleteListener {

    private final UserDeleteService userDeleteService;

    @RabbitListener(queues = UserDeleteRabbitConfig.USER_DELETE_QUEUE, containerFactory = "userDeleteListenerContainerFactory")
    public void onUserDeleted(UserDeletedEvent message) {
        try {
            userDeleteService.disableUserInEntra(message.userOid());
        } catch (RetryableUserDeleteSyncException ex) {
            log.warn("Retryable Entra disable failure userOid={} email={}", message.userOid(), message.email(), ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Unexpected Entra disable failure userOid={} email={}", message.userOid(), message.email(), ex);
            throw ex;
        }
    }
}
