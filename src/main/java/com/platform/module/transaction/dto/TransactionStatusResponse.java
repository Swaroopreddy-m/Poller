package com.platform.module.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionStatusResponse {

    private String refNo;
    private UUID clientId;
    private String requestType;
    private String processingMode;
    private String overallStatus;
    private String completionOutcome;

    private ServiceCounts serviceCounts;
    private ProcessingMetrics metrics;
    private TemporalMilestones timestamps;
    private Map<String, String> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceCounts {
        private int total;
        private int sms;
        private int email;
        private int pdf;
        private int excel;
        private int csv;
        private int report;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingMetrics {
        private int success;
        private int failed;
        private int inProgress;
        private int completed;
        private int expired;
        private int paused;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemporalMilestones {
        private Instant createdAt;
        private Instant startedAt;
        private Instant completedAt;
        private Instant lastUpdatedAt;
        private Instant expiresAt;
        private Instant retentionUntil;
    }
}
