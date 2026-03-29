package com.admina.api.service.ai.gemini;

import com.admina.api.dto.ai.gemini.SummarizeResponse;
import com.admina.api.dto.ai.gemini.TranslateResponse;

public interface GeminiService {
    TranslateResponse translateDocument(byte[] fileBytes, String mimeType, String targetLanguage);
    SummarizeResponse summarizeDocument(String translatedText, String targetLanguage);
}
