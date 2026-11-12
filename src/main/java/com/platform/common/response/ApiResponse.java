package com.platform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private String refNo;
    private T data;
    private List<ValidationErrorItem> errors;
    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data, String message, String refNo) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .refNo(refNo)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String refNo, List<ValidationErrorItem> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .refNo(refNo)
                .errors(errors)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationErrorItem {
        private String field;
        private String code;
        private String message;
        private Object rejectedValue;
    }
}
