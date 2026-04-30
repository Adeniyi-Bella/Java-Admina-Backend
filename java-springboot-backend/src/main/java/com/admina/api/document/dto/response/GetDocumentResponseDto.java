package com.admina.api.document.dto.response;

import com.admina.api.document.dto.ActionPlanTaskDto;
import com.admina.api.document.model.ActionPlanItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GetDocumentResponseDto(
                UUID id,
                String targetLanguage,
                String title,
                String sender,
                String receivedDate,
                String summary,
                Map<String, String> structuredTranslatedText,
                List<ActionPlanItem> actionPlan,
                List<ActionPlanTaskDto> actionPlanTasks,
                List<ChatMessageResponseDto> chatMessagesHistory,
                int chatbotCreditRemaining) {
}
