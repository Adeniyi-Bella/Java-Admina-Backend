package com.admina.api.document.dto.response;

import java.util.List;
import java.util.UUID;

public record GetDocumentsPageDto(
        List<DocumentSummary> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {

    public record DocumentSummary(
            UUID id,
            String title,
            String sender,
            String receivedDate,
            long completedTasksCount,
            long uncompletedTasksCount) {
    }
}
