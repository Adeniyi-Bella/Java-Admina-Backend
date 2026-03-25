package com.admina.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ResponseDtos.ErrorResponse error;
    private final String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .error(null)
            .timestamp(OffsetDateTime.now().toString())
            .build();
    }

    public static ApiResponse<ResponseDtos.ErrorResponse> error(ResponseDtos.ErrorResponse error) {
        return ApiResponse.<ResponseDtos.ErrorResponse>builder()
            .success(false)
            .data(null)
            .error(error)
            .timestamp(OffsetDateTime.now().toString())
            .build();
    }
}
