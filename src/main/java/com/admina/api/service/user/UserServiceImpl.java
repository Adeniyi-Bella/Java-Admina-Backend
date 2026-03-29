package com.admina.api.service.user;

import com.admina.api.dto.user.UserCreatedEvent;
import com.admina.api.dto.user.UserDto;
import com.admina.api.model.User;
import com.admina.api.model.UserRole;
import com.admina.api.repository.UserRepository;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.exceptions.AppExceptions;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserDto authenticate(AuthenticatedPrincipal principal) {
        validatePrincipal(principal);
        User user = userRepository.findByEmail(principal.getEmail())
                .orElseGet(() -> createUser(principal));
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getExistingUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("User could not be found"));
    }

    private User createUser(AuthenticatedPrincipal principal) {
        User user = User.builder()
                .email(principal.getEmail())
                .oid(principal.getOid())
                .username(principal.getUsername())
                .role(UserRole.ROLE_USER)
                .build();
        try {
            User saved = userRepository.saveAndFlush(user);
            eventPublisher.publishEvent(new UserCreatedEvent(saved));
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return userRepository.findByEmail(principal.getEmail())
                    .orElseThrow(() -> new AppExceptions.ConflictException(
                            "User registration conflict for email: " + principal.getEmail()));
        }
    }

    // This guard is a defence-in-depth check at the service boundary.
    private void validatePrincipal(AuthenticatedPrincipal principal) {
        Assert.hasText(principal.getOid(), "Principal OID must not be blank");
        Assert.hasText(principal.getEmail(), "Principal email must not be blank");
        Assert.hasText(principal.getUsername(), "Principal username must not be blank");
    }
}
