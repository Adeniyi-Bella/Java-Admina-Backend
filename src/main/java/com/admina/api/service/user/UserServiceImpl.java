package com.admina.api.service.user;

import com.admina.api.config.properties.AppPlanProperties;
import com.admina.api.dto.document.DocumentDto;
import com.admina.api.dto.user.UserDto;
import com.admina.api.dto.user.UserWithDocumentsResponse;
import com.admina.api.enums.PlanType;
import com.admina.api.events.user.UserCreatedEvent;
import com.admina.api.model.user.User;
import com.admina.api.model.user.UserRole;
import com.admina.api.repository.DocumentRepository;
import com.admina.api.repository.UserRepository;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.service.document.DocumentMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;
    private final AppPlanProperties appPlanProperties;

    @Transactional
    @Override
    public UserWithDocumentsResponse authenticate(AuthenticatedPrincipal principal) {
        validatePrincipal(principal);
        User user = userRepository.findByEmail(principal.getEmail())
                .orElseGet(() -> createUser(principal));
        List<DocumentDto> documents = documentRepository
            .findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .map(documentMapper::toDto)
            .toList();
        return new UserWithDocumentsResponse(userMapper.toDto(user), documents);
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
                .planLimitMax(appPlanProperties.getMaxForPlan(PlanType.FREE))
                .planLimitCurrent(appPlanProperties.getMaxForPlan(PlanType.FREE))
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
