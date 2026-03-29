package com.admina.api.service.document;

import com.admina.api.dto.ai.gemini.SummarizeResponse;
import com.admina.api.dto.ai.gemini.TranslateResponse;
import com.admina.api.events.document.DocumentCreateEvent;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.model.document.ActionPlanTask;
import com.admina.api.model.document.Document;
import com.admina.api.model.user.User;
import com.admina.api.repository.DocumentRepository;
import com.admina.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPersistenceServiceImpl implements DocumentPersistenceService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public void createDocumentAndDecrementLimit(
            DocumentCreateEvent message,
            TranslateResponse translated,
            SummarizeResponse summarized) {

        // 1. Fetch user
        User user = userRepository.findById(message.userId())
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException(
                        "User not found for document creation"));

        // 2. Build and save document
        List<ActionPlanTask> tasks = summarized.actionPlans().stream()
                .map(task -> ActionPlanTask.builder()
                        .title(task.title())
                        .dueDate(task.dueDate())
                        .completed(task.completed())
                        .location(task.location())
                        .build())
                .toList();

        Document document = Document.builder()
                .id(message.docId())
                .user(user)
                .targetLanguage(message.targetLanguage())
                .title(summarized.title())
                .sender(summarized.sender())
                .receivedDate(summarized.receivedDate())
                .summary(summarized.summary())
                .translatedText(translated.translatedText())
                .structuredTranslatedText(translated.structuredTranslatedText())
                .actionPlan(summarized.actionPlan())
                .actionPlanTasks(tasks)
                .build();

        tasks.forEach(task -> task.setDocument(document));
        documentRepository.save(document);

        user.setPlanLimitCurrent(user.getPlanLimitCurrent() - 1);
        userRepository.save(user);

        log.info("Document created and limit decremented userId={} docId={} remainingLimit={}",
                message.userId(), message.docId(), user.getPlanLimitCurrent() - 1);
    }
}