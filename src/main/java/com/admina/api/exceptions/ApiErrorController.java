package com.admina.api.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;

import com.admina.api.exceptions.ResponseDtos.ErrorResponse;

import java.time.OffsetDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ApiErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    @RequestMapping("/error")
    public ResponseEntity<CustomApiResponse<ErrorResponse>> handleError(HttpServletRequest request) {
        Map<String, Object> attrs = errorAttributes.getErrorAttributes(
            new ServletWebRequest(request),
            ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE)
        );
        int status = (int) attrs.getOrDefault("status", 500);
        String message = (String) attrs.getOrDefault("message", "Unexpected error");
        ErrorResponse error = ErrorResponse.builder()
            .status(status)
            .message(message)
            .timestamp(OffsetDateTime.now().toString())
            .build();
        return ResponseEntity.status(HttpStatus.valueOf(status)).body(CustomApiResponse.error(error));
    }
}
