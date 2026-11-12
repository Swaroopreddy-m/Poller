package com.platform.common.exception;

import com.platform.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlatformException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlatformException(PlatformException ex) {
        log.error("Platform business exception: {} - Code: {}", ex.getMessage(), ex.getErrorCode());
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), ex.getRefNo(), null);
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiResponse.ValidationErrorItem> errorItems = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ApiResponse.ValidationErrorItem.builder()
                        .field(fieldError.getField())
                        .code(fieldError.getCode())
                        .message(fieldError.getDefaultMessage())
                        .rejectedValue(fieldError.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ApiResponse<Void> response = ApiResponse.error("Input validation failed", null, errorItems);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled platform exception", ex);
        // Never leak internal stack trace to external clients
        ApiResponse<Void> response = ApiResponse.error("An internal system error occurred. Please contact support.", null, null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
