package com.admina.api.sub;

import com.admina.api.events.notification.SendWelcomeEmailEvent;
import com.admina.api.events.user.UserCreatedEvent;
import com.admina.api.model.user.User;
import com.admina.api.pub.SendWelcomeEmailPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final SendWelcomeEmailPublisher notificationPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        User user = event.user();
        notificationPublisher.publishWelcome(new SendWelcomeEmailEvent(
                user.getId(),
                user.getEmail(),
                user.getUsername()
        ));
    }
}
