package com.admina.api.exceptions;

import com.admina.api.dto.response.ApiResponse;
import com.admina.api.dto.response.ResponseDtos.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class HttpErrorResponder {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int status, String message) throws IOException {
        ErrorResponse error = ErrorResponse.builder()
            .status(status)
            .message(message)
            .timestamp(OffsetDateTime.now().toString())
            .build();

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(error)));
    }
}