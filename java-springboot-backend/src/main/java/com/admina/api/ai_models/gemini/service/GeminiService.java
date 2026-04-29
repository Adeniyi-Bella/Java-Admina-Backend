package com.admina.api.ai_models.gemini.service;

import com.admina.api.ai_models.gemini.dto.SummarizeResponse;
import com.admina.api.ai_models.gemini.dto.TranslateResponse;

import java.util.List;

public interface GeminiService {
    TranslateResponse translateDocument(byte[] fileBytes, String mimeType, String targetLanguage);
    SummarizeResponse summarizeDocument(String translatedText, String targetLanguage);
    String generateChatbotResponse(
            String translatedText,
            List<ChatHistoryMessage> history,
            String userPrompt);

    record ChatHistoryMessage(String role, String content) {}
}
