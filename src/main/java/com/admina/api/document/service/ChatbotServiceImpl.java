package com.admina.api.document.service;

import com.admina.api.document.dto.request.ChatbotPromptRequest;
import com.admina.api.document.dto.response.ChatJobResponse;
import com.admina.api.document.dto.response.ChatJobStatusResponse;
import com.admina.api.document.enums.ChatProcessStatus;
import com.admina.api.document.model.ChatMessage;
import com.admina.api.document.model.Document;
import com.admina.api.document.repository.ChatMessageRepository;
import com.admina.api.document.repository.DocumentRepository;
import com.admina.api.document.events.ChatJobEvent;
import com.admina.api.document.pub.ChatJobPublisher;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.redis.RedisService;
import com.admina.api.security.auth.AuthenticatedPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

        private static final String ROLE_USER = "USER";

        private final DocumentRepository documentRepository;
        private final ChatMessageRepository chatMessageRepository;
        private final ChatJobPublisher chatJobPublisher;
        private final RedisService redisService;

        @Override
        public ChatJobResponse createChatJob(
                        AuthenticatedPrincipal principal,
                        UUID docId,
                        ChatbotPromptRequest request) {

                String prompt = normalizePrompt(request.prompt());
                Document document = documentRepository.findDocumentByIdAndUserEmail(docId, principal.getEmail())
                                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Document not found"));

                ChatMessage userMessage = chatMessageRepository
                                .findTopByDocumentIdAndRoleOrderByCreatedAtDesc(docId, ROLE_USER)
                                .filter(last -> normalizePrompt(last.getContent()).equals(prompt))
                                .orElseGet(() -> chatMessageRepository.saveAndFlush(ChatMessage.builder()
                                                .document(document)
                                                .role(ROLE_USER)
                                                .content(prompt)
                                                .build()));

                UUID chatbotPollingId = UUID.randomUUID();
                redisService.setChatJobStatus(chatbotPollingId, docId, ChatProcessStatus.PENDING, null, null);

                try {
                        chatJobPublisher.publish(new ChatJobEvent(
                                        chatbotPollingId,
                                        docId,
                                        principal.getEmail(),
                                        prompt,
                                        userMessage.getId()));
                } catch (Exception ex) {
                        log.error("Failed to queue chat job chatbotPollingId={} docId={}", chatbotPollingId, docId, ex);
                        redisService.setChatJobStatus(chatbotPollingId, docId, ChatProcessStatus.ERROR, ex.getMessage(),
                                        null);
                        throw new AppExceptions.ServiceUnavailableException("Chat processing is unavailable");
                }

                return new ChatJobResponse(chatbotPollingId, docId, ChatProcessStatus.PENDING);
        }

        @Override
        public ChatJobStatusResponse getChatJobStatus(
                        AuthenticatedPrincipal principal,
                        UUID docId,
                        UUID chatbotPollingId) {

                documentRepository.findDocumentByIdAndUserEmail(docId, principal.getEmail())
                                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Document not found"));

                ChatJobStatusResponse status = redisService.getChatJobStatus(chatbotPollingId)
                                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Chat job not found in Redis"));

                if (!chatbotPollingId.equals(status.chatbotPollingId())) {
                        throw new AppExceptions.ResourceNotFoundException("Chat job not found");
                }

                return status;
        }

        private String normalizePrompt(String prompt) {
                if (prompt == null || prompt.isBlank()) {
                        throw new AppExceptions.BadRequestException("Prompt is required");
                }
                return prompt.strip();
        }
}
