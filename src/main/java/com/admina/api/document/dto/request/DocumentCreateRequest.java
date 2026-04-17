package com.admina.api.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocumentCreateRequest(
    @NotBlank(message = "Document language is required")
    @Size(min = 2, max = 5, message = "Must be 2–5 characters")
    @Pattern(regexp = "^[a-zA-Z]{2,5}$", message = "Must be a valid language code")
    String docLanguage,
    
    @NotBlank(message = "Target language is required")
    @Size(min = 2, max = 5, message = "Must be 2–5 characters")
    @Pattern(regexp = "^[a-zA-Z]{2,5}$", message = "Must be a valid language code")
    String targetLanguage
) {
}
