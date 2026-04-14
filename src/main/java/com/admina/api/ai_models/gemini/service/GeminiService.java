package com.admina.api.ai_models.gemini.service;

import com.admina.api.ai_models.gemini.dto.SummarizeResponse;
import com.admina.api.ai_models.gemini.dto.TranslateResponse;

public interface GeminiService {
    TranslateResponse translateDocument(byte[] fileBytes, String mimeType, String targetLanguage);
    SummarizeResponse summarizeDocument(String translatedText, String targetLanguage);
}
