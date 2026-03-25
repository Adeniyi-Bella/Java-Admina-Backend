package com.admina.api.service.user;

import com.admina.api.dto.user.UserDto;
import com.admina.api.model.User;
import com.admina.api.model.UserRole;
import com.admina.api.notifications.NotificationMessage;
import com.admina.api.service.notification.NotificationPublisher;
import com.admina.api.repository.UserRepository;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.exceptions.AppExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    @Override
    public UserDto authenticate(AuthenticatedPrincipal principal) {
        User user = userRepository.findByEmail(principal.getEmail())
            .orElseGet(() -> createUser(principal));
        return toDto(user);
    }

    @Override
    public User getExistingUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new AppExceptions.UnauthorizedException("User not registered"));
    }

    
    private User createUser(AuthenticatedPrincipal principal) {
        User user = User.builder()
            .email(principal.getEmail())
            .oid(principal.getOid())
            .username(principal.getUsername())
            .role(UserRole.ROLE_USER)
            .build();
        try {
            User saved = userRepository.save(user);
            notificationPublisher.publishWelcome(new NotificationMessage(
                saved.getId(),
                saved.getEmail(),
                saved.getUsername()
            ));
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> ex);
        }
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .oid(user.getOid())
            .username(user.getUsername())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
