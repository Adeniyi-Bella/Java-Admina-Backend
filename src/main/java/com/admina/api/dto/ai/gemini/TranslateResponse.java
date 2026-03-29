package com.admina.api.dto.ai.gemini;

import java.util.Map;

public record TranslateResponse(
        String translatedText,
        Map<String, String> structuredTranslatedText) {
}
