package com.admina.api.exceptions;

import com.admina.api.dto.response.ApiResponse;
import com.admina.api.dto.response.ResponseDtos.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppExceptions.ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFound(
            AppExceptions.ResourceNotFoundException ex,
            HttpServletRequest request) {
        logClientError("NOT_FOUND", request, ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.ConflictException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConflict(
            AppExceptions.ConflictException ex,
            HttpServletRequest request) {
        logClientError("CONFLICT", request, ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.ForbiddenException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleForbidden(
            AppExceptions.ForbiddenException ex,
            HttpServletRequest request) {
        logClientError("FORBIDDEN", request, ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.UnauthorizedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnauthorized(
            AppExceptions.UnauthorizedException ex,
            HttpServletRequest request) {
        logClientError("UNAUTHORIZED", request, ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.BadRequestException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBadRequest(
            AppExceptions.BadRequestException ex,
            HttpServletRequest request) {
        logClientError("BAD_REQUEST", request, ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleServiceUnavailable(
            AppExceptions.ServiceUnavailableException ex,
            HttpServletRequest request) {
        logClientError("SERVICE_UNAVAILABLE", request, ex.getMessage());
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleTooManyRequests(
            AppExceptions.TooManyRequestsException ex,
            HttpServletRequest request) {
        logClientError("TOO_MANY_REQUESTS", request, ex.getMessage());
        return buildError(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.InternalServerErrorException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInternalServerError(
            AppExceptions.InternalServerErrorException ex,
            HttpServletRequest request) {
        log.error("Internal server error path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.BadGatewayException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBadGateway(
            AppExceptions.BadGatewayException ex,
            HttpServletRequest request) {
        log.error("Bad gateway error path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(AppExceptions.GatewayTimeoutException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGatewayTimeout(
            AppExceptions.GatewayTimeoutException ex,
            HttpServletRequest request) {
        log.error("Gateway timeout error path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        logClientError("VALIDATION_FAILED", request, message);
        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        logClientError("ROUTE_NOT_FOUND", request, "Route not found");
        return buildError(HttpStatus.NOT_FOUND, "Route not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        logClientError("METHOD_NOT_ALLOWED", request, "Method not allowed");
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        logClientError("UNSUPPORTED_MEDIA_TYPE", request, "Content-Type is not supported");
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type is not supported");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDatabase(DataAccessException ex) {
        log.error("Database error", ex);
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, "Database unavailable");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleFileSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        logClientError("FILE_TOO_LARGE", request, "File size exceeds the maximum allowed (10MB)");
        return buildError(HttpStatus.BAD_REQUEST, "File size exceeds the maximum allowed (10MB)");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGeneric(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> buildError(HttpStatus status, String message) {
        ErrorResponse error = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(OffsetDateTime.now().toString())
                .build();
        return ResponseEntity.status(status).body(ApiResponse.error(error));
    }

    private void logClientError(String code, HttpServletRequest request, String message) {
        log.warn("client_error code={} method={} path={} message={}",
                code,
                request.getMethod(),
                request.getRequestURI(),
                message);
    }
}
