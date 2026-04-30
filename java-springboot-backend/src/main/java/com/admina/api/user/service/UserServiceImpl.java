package com.admina.api.user.service;

import com.admina.api.document.dto.ActionPlanTaskDto;
import com.admina.api.document.model.ActionPlanTask;
import com.admina.api.document.model.Document;
import com.admina.api.document.repository.DocumentRepository;
import com.admina.api.security.auth.AuthenticatedPrincipal;
import com.admina.api.user.dto.UserAuthenticationResult;
import com.admina.api.user.dto.UserDocumentDto;
import com.admina.api.user.dto.UserDto;
import com.admina.api.user.dto.response.UserResponseDto;
import com.admina.api.user.enums.PlanType;
import com.admina.api.user.events.UserCreatedEvent;
import com.admina.api.user.model.User;
import com.admina.api.user.model.UserRole;
import com.admina.api.user.repository.ActionPlanTaskRepository;
import com.admina.api.user.repository.UserRepository;
import com.admina.api.exceptions.AppExceptions;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ActionPlanTaskRepository actionPlanTaskRepository;
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserAuthenticationResult authenticate(AuthenticatedPrincipal principal) {
        validatePrincipal(principal);
        UserCreationResult newUser = userRepository.findByEmail(principal.getEmail())
                .map(user -> new UserCreationResult(user, false))
                .orElseGet(() -> createUser(principal));

        User user = newUser.user();

        List<UserDocumentDto> documents = getRecentUserDocumentsWithTasks(user.getId());
        List<ActionPlanTaskDto> upcomingTasks = getUpcomingTasks(user.getId());
        int totalDocuments = Math.toIntExact(documentRepository.countByUserId(user.getId()));
        int pendingActions = Math.toIntExact(actionPlanTaskRepository.countByUserIdAndCompletedFalse(user.getId()));
        int completedTasks = Math.toIntExact(actionPlanTaskRepository.countByUserIdAndCompletedTrue(user.getId()));
        LocalDate today = LocalDate.now();
        int upcomingDeadlines = Math.toIntExact(actionPlanTaskRepository.countUpcomingByUserId(
                user.getId(),
                today,
                today.plusDays(30)));

        UserResponseDto.UserWithDocumentsResponseDto response = new UserResponseDto.UserWithDocumentsResponseDto(
                new UserResponseDto.GetOrCreateUserResponseDto(
                        user.getEmail(),
                        user.getUsername(),
                        user.getRole(),
                        user.getPlan(),
                        user.getDocumentsUsed(),
                        totalDocuments,
                        pendingActions,
                        upcomingDeadlines,
                        completedTasks),
                documents,
                upcomingTasks);

        return new UserAuthenticationResult(response, newUser.created());
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getExistingUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("User could not be found"));
    }

    @Transactional
    @Override
    public void updateStripeCustomerId(String email, String stripeCustomerId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("User not found"));
        user.setStripeCustomerId(stripeCustomerId);
        userRepository.save(user);
    }

    private UserCreationResult createUser(AuthenticatedPrincipal principal) {
        User user = User.builder()
                .email(principal.getEmail())
                .oid(principal.getOid())
                .username(principal.getUsername())
                .plan(PlanType.FREE)
                .role(UserRole.ROLE_USER)
                .documentsUsed(PlanType.FREE.getMaxDocuments())
                .build();
        try {
            User saved = userRepository.saveAndFlush(user);
            eventPublisher.publishEvent(new UserCreatedEvent(saved));
            return new UserCreationResult(saved, true);
        } catch (DataIntegrityViolationException ex) {
            User existing = userRepository.findByEmail(principal.getEmail())
                    .orElseThrow(() -> new AppExceptions.ConflictException(
                            "User registration conflict for email: " + principal.getEmail()));
            return new UserCreationResult(existing, false);
        }
    }

    // This guard is a defence-in-depth check at the service boundary.
    private void validatePrincipal(AuthenticatedPrincipal principal) {
        if (principal == null
                || !StringUtils.hasText(principal.getOid())
                || !StringUtils.hasText(principal.getEmail())
                || !StringUtils.hasText(principal.getUsername())) {
            throw new AppExceptions.UnauthorizedException();
        }
    }

    private ActionPlanTaskDto toTaskDto(ActionPlanTask task) {
        return new ActionPlanTaskDto(
                task.getId(),
                task.getTitle(),
                task.getDueDate(),
                task.isCompleted(),
                task.getLocation(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    private UserDocumentDto toDocumentDto(Document document, List<ActionPlanTaskDto> tasks) {
        return new UserDocumentDto(
                document.getId(),
                document.getTitle(),
                document.getSender(),
                document.getReceivedDate(),
                List.copyOf(tasks),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private List<UserDocumentDto> getRecentUserDocumentsWithTasks(UUID userId) {
        List<UUID> documentIds = documentRepository.findTopDocumentIdsByUserId(userId, PageRequest.of(0, 3));
        if (documentIds.isEmpty()) {
            return List.of();
        }

        List<Document> documentsWithTasks = documentRepository.findAllWithTasksByIdIn(documentIds);
        Map<UUID, Document> documentsById = new LinkedHashMap<>();
        for (Document document : documentsWithTasks) {
            documentsById.put(document.getId(), document);
        }

        return documentIds.stream()
                .map(docId -> documentsById.get(docId))
                .filter(document -> document != null)
                .map(document -> toDocumentDto(
                        document,
                        document.getActionPlanTasks().stream()
                                .map(this::toTaskDto)
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    private List<ActionPlanTaskDto> getUpcomingTasks(UUID userId) {
        return actionPlanTaskRepository
                .findTop3ByUserIdAndCompletedFalseAndDueDateIsNotNullOrderByDueDateAscCreatedAtAsc(userId)
                .stream()
                .map(this::toTaskDto)
                .collect(Collectors.toList());
    }

    private record UserCreationResult(User user, boolean created) {
    }
}
