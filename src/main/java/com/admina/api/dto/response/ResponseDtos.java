package com.admina.api.dto.response;

import lombok.Builder;
import lombok.Data;

public class ResponseDtos {

    @Data
    @Builder
    public static class ErrorResponse {
        private int status;
        private String message;
        private String timestamp;
    }
}
