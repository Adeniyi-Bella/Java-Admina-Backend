package com.admina.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import com.admina.api.dto.document.DocumentDto;

public class ResponseDtos {

    @Data
    @Builder
    public static class DocumentUploadResponse {
        private String docId;
        private String status;
    }

    @Data
    @Builder
    public static class DocumentStatusResponse {
        private String docId;
        private String status;
    }

    @Data
    @Builder
    public static class DocumentListResponse {
        private List<DocumentDto> documents;
        private PaginationInfo pagination;
    }

    @Data
    @Builder
    public static class PaginationInfo {
        private String nextCursor;
        private boolean hasMore;
    }

    @Data
    @Builder
    public static class CreatePaymentIntentResponse {
        private String paymentIntentId;
        private String clientSecret;
        private Integer amount;
    }

    @Data
    @Builder
    public static class ConfirmPaymentResponse {
        private boolean success;
    }

    @Data
    @Builder
    public static class ErrorResponse {
        private int status;
        private String message;
        private String timestamp;
    }
}
