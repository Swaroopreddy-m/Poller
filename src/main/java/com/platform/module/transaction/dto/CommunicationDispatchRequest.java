package com.platform.module.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommunicationDispatchRequest {

    @NotBlank(message = "requestType is mandatory")
    private String requestType;

    private String idempotencyKey;

    @Builder.Default
    private Integer priority = 5;

    @Builder.Default
    private Integer ttlSeconds = 86400;

    private Operations operations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Operations {
        private List<EmailOperation> email;
        private List<SmsOperation> sms;
        private List<DocumentOperation> documents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailOperation {
        private String recipient;
        private String cc;
        private String bcc;
        private String subject;
        private String content;
        private Map<String, Object> parameters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsOperation {
        private String recipientPhone;
        private String content;
        private Map<String, Object> parameters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentOperation {
        private String documentType; // PDF, EXCEL, CSV
        private String fileName;
        private String templateContent;
        private Map<String, Object> parameters;
    }
}
