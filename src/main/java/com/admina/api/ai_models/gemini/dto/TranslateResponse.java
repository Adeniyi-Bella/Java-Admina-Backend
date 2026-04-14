package com.admina.api.ai_models.gemini.dto;

import java.util.Map;

public record TranslateResponse(
        String translatedText,
        Map<String, String> structuredTranslatedText) {
}
