package com.admina.api.document.sub;

import com.admina.api.ai_models.gemini.service.GeminiService;
import com.admina.api.config.rabbit.ChatRabbitConfig;
import com.admina.api.document.enums.ChatProcessStatus;
import com.admina.api.document.events.ChatJobEvent;
import com.admina.api.document.model.ChatMessage;
import com.admina.api.document.model.Document;
import com.admina.api.document.repository.ChatMessageRepository;
import com.admina.api.document.repository.DocumentRepository;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.redis.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatJobListener {

        private static final String ROLE_AGENT = "AGENT";

        private final RedisService redisService;
        private final GeminiService geminiService;
        private final DocumentRepository documentRepository;
        private final ChatMessageRepository chatMessageRepository;

        @Transactional
        @RabbitListener(queues = ChatRabbitConfig.CHAT_QUEUE, containerFactory = "chatListenerContainerFactory")
        public void handle(ChatJobEvent message) {
                var current = redisService.getChatJobStatus(message.chatbotPollingId());
                if (current.isEmpty()) {
                        log.warn("Dropping chat job with expired status chatbotPollingId={} docId={}",
                                        message.chatbotPollingId(), message.docId());
                        return;
                }
                if (current.get().status() == ChatProcessStatus.COMPLETED
                                || current.get().status() == ChatProcessStatus.ERROR) {
                        log.info("Dropping duplicate finished chat job chatbotPollingId={} docId={} status={}",
                                        message.chatbotPollingId(), message.docId(), current.get().status());
                        return;
                }

                try {
                        redisService.setChatJobStatus(message.chatbotPollingId(), message.docId(),
                                        ChatProcessStatus.PROCESSING, null, null);
                        log.info("Processing chat job chatbotPollingId={} docId={}", message.chatbotPollingId(),
                                        message.docId());

                        Document document = documentRepository
                                        .findDocumentByIdAndUserEmail(message.docId(), message.userEmail())
                                        .orElseThrow(() -> new AppExceptions.ResourceNotFoundException(
                                                        "Document not found"));

                        String translatedText = document.getTranslatedText();

                        List<GeminiService.ChatHistoryMessage> history = chatMessageRepository
                                        .findAllByDocumentIdOrderByCreatedAtAscIdAsc(message.docId())
                                        .stream()
                                        .map(chat -> new GeminiService.ChatHistoryMessage(chat.getRole(),
                                                        chat.getContent()))
                                        .toList();

                        String responseText = geminiService.generateChatbotResponse(
                                        translatedText,
                                        history,
                                        message.prompt());

                        redisService.setChatJobStatus(message.chatbotPollingId(), message.docId(),
                                        ChatProcessStatus.SAVING, null, null);

                        log.info("Saving chat job chatbotPollingId={} docId={}", message.chatbotPollingId(),
                                        message.docId());

                        chatMessageRepository.saveAndFlush(ChatMessage.builder()
                                        .document(document)
                                        .role(ROLE_AGENT)
                                        .content(responseText)
                                        .build());

                        if (document.getChatbotCreditRemaining() > 0) {
                                document.setChatbotCreditRemaining(document.getChatbotCreditRemaining() - 1);
                        }
                        documentRepository.saveAndFlush(document);

                        redisService.setChatJobStatus(message.chatbotPollingId(), message.docId(),
                                        ChatProcessStatus.COMPLETED, null, responseText);
                        log.info("Chat job completed chatbotPollingId={} docId={} userMessageId={}",
                                        message.chatbotPollingId(), message.docId(), message.userMessageId());
                } catch (AppExceptions.ResourceNotFoundException | AppExceptions.BadRequestException ex) {
                        log.warn("Chat job failed chatbotPollingId={} docId={} userMessageId={} message={}",
                                        message.chatbotPollingId(), message.docId(), message.userMessageId(),
                                        ex.getMessage());
                        markFailed(message, ex);
                } catch (Exception ex) {
                        log.error("Chat job failed chatbotPollingId={} docId={} userMessageId={}",
                                        message.chatbotPollingId(), message.docId(), message.userMessageId(), ex);
                        markFailed(message, ex);
                }
        }

        private void markFailed(ChatJobEvent message, Exception ex) {
                redisService.setChatJobStatus(
                                message.chatbotPollingId(),
                                message.docId(),
                                ChatProcessStatus.ERROR,
                                resolveErrorMessage(ex),
                                null);
        }

        private String resolveErrorMessage(Exception ex) {
                String message = ex.getMessage();
                if (message == null || message.isBlank()) {
                        return "Chat job failed";
                }
                return message;
        }
}
