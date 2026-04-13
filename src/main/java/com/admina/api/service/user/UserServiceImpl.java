package com.admina.api.service.user;

import com.admina.api.dto.tasks.ActionPlanTaskDto;
import com.admina.api.dto.user.UserAuthenticationResult;
import com.admina.api.dto.user.UserDocumentDto;
import com.admina.api.dto.user.UserDto;
import com.admina.api.dto.user.UserWithDocumentsResponseDto;
import com.admina.api.enums.PlanType;
import com.admina.api.events.user.UserCreatedEvent;
import com.admina.api.model.document.Document;
import com.admina.api.model.task.ActionPlanTask;
import com.admina.api.model.user.User;
import com.admina.api.model.user.UserRole;
import com.admina.api.repository.ActionPlanTaskRepository;
import com.admina.api.repository.DocumentRepository;
import com.admina.api.repository.UserRepository;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.exceptions.AppExceptions;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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

        List<UserDocumentDto> documents = List.of();

        if (!newUser.created()) {
            documents = getExistingUserDocumentsWithTasks(user.getId());
        }

        UserWithDocumentsResponseDto response = new UserWithDocumentsResponseDto(
                userMapper.toDto(user),
                documents);

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

    private List<UserDocumentDto> getDocumentsLinkedToTasks(List<ActionPlanTask> tasks) {
        // nothing to map if no tasks
        if (tasks.isEmpty()) {
            return List.of();
        }

        // extract unique document IDs in the order they appear in the task list
        // LinkedHashSet preserves insertion order and removes duplicates
        // (multiple tasks can belong to the same document)
        LinkedHashSet<UUID> orderedDocumentIds = tasks.stream()
                .map(task -> task.getDocument().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // fetch all documents in one query instead of one query per document (avoids
        // N+1)
        List<Document> taskDocuments = documentRepository.findAllById(orderedDocumentIds);

        // index documents by ID for O(1) lookup later instead of looping every time
        Map<UUID, Document> documentsById = taskDocuments.stream()
                .collect(Collectors.toMap(Document::getId, document -> document));

        // group tasks by their document ID, preserving order with LinkedHashMap
        // computeIfAbsent creates a new list for a document ID if one doesn't exist yet
        Map<UUID, List<ActionPlanTaskDto>> tasksByDocumentId = new LinkedHashMap<>();
        for (ActionPlanTask task : tasks) {
            UUID documentId = task.getDocument().getId();
            tasksByDocumentId
                    .computeIfAbsent(documentId, ignored -> new ArrayList<>())
                    .add(toTaskDto(task));
        }

        // build the final result in the original task order
        List<UserDocumentDto> result = new ArrayList<>();
        for (UUID documentId : orderedDocumentIds) {
            Document document = documentsById.get(documentId);
            if (document == null) {
                // skip if document was deleted between fetching tasks and fetching documents
                continue;
            }
            // get tasks for this document, defaulting to empty list if none found
            List<ActionPlanTaskDto> documentTasks = tasksByDocumentId.getOrDefault(documentId, List.of());
            result.add(toDocumentDto(document, documentTasks));
        }
        return result;
    }

    private List<UserDocumentDto> getExistingUserDocumentsWithTasks(UUID userId) {
        var tasks = actionPlanTaskRepository.findTop3ByUserIdAndCompletedFalseOrderByDueDateAsc(userId);
        return getDocumentsLinkedToTasks(tasks);
    }

    private record UserCreationResult(User user, boolean created) {
    }
}
