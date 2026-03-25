package com.admina.api.dto.document;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocumentCreateRequest(
    @Size(min = 2, max = 5, message = "Must be 2–5 characters")
    @Pattern(regexp = "^[a-zA-Z]{2,5}$", message = "Must be a valid language code")
    String docLanguage,
    @Size(min = 2, max = 5, message = "Must be 2–5 characters")
    @Pattern(regexp = "^[a-zA-Z]{2,5}$", message = "Must be a valid language code")
    String targetLanguage
) {
}
