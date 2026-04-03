package com.admina.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class CustomApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ResponseDtos.ErrorResponse error;
    private final String timestamp;

    public static <T> CustomApiResponse<T> success(T data) {
        return CustomApiResponse.<T>builder()
            .success(true)
            .data(data)
            .error(null)
            .timestamp(OffsetDateTime.now().toString())
            .build();
    }

    public static CustomApiResponse<ResponseDtos.ErrorResponse> error(ResponseDtos.ErrorResponse error) {
        return CustomApiResponse.<ResponseDtos.ErrorResponse>builder()
            .success(false)
            .data(null)
            .error(error)
            .timestamp(OffsetDateTime.now().toString())
            .build();
    }
}
