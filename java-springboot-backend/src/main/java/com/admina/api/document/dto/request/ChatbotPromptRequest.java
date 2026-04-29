package com.admina.api.document.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatbotPromptRequest(
        @NotBlank(message = "Prompt is required")
        String prompt) {
}
