package com.admina.api.service.user;

import com.admina.api.dto.notification.NotificationMessage;
import com.admina.api.dto.user.UserCreatedEvent;
import com.admina.api.model.User;
import com.admina.api.service.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final NotificationPublisher notificationPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        User user = event.user();
        notificationPublisher.publishWelcome(new NotificationMessage(
                user.getId(),
                user.getEmail(),
                user.getUsername()
        ));
    }
}
