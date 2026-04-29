package com.admina.api.exceptions;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
public class CustomApiResponse<T> {
    private final boolean success;

    @Schema(nullable = true, description = "Present on success, null on error")
    private final T data;

    @Schema(nullable = true, description = "Present on error, null on success")
    private final ResponseDtos.ErrorResponse error;

    private final OffsetDateTime timestamp;

    public static <T> CustomApiResponse<T> success(T data) {
        return CustomApiResponse.<T>builder()
                .success(true)
                .data(data)
                .error(null)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static CustomApiResponse<ResponseDtos.ErrorResponse> error(ResponseDtos.ErrorResponse error) {
        return CustomApiResponse.<ResponseDtos.ErrorResponse>builder()
                .success(false)
                .data(null)
                .error(error)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
