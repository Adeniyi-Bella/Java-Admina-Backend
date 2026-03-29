package com.admina.api.service.document;

import com.admina.api.dto.ai.gemini.SummarizeResponse;
import com.admina.api.dto.ai.gemini.TranslateResponse;
import com.admina.api.events.document.DocumentCreateEvent;

public interface DocumentPersistenceService {
    void createDocumentAndDecrementLimit(DocumentCreateEvent message, TranslateResponse translated, SummarizeResponse summarized);
}