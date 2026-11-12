package com.platform.module.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "out_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "ref_no", nullable = false, length = 36)
    private String refNo;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "request_type", nullable = false, length = 64)
    private String requestType;

    @Column(name = "request_source", nullable = false, length = 64)
    @Builder.Default
    private String requestSource = "REST_API";

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "processing_mode", nullable = false, length = 16)
    private String processingMode; // SYNC, ASYNC

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 5;

    // Service Counts
    @Column(name = "sms_count", nullable = false)
    @Builder.Default
    private Integer smsCount = 0;

    @Column(name = "email_count", nullable = false)
    @Builder.Default
    private Integer emailCount = 0;

    @Column(name = "pdf_count", nullable = false)
    @Builder.Default
    private Integer pdfCount = 0;

    @Column(name = "excel_count", nullable = false)
    @Builder.Default
    private Integer excelCount = 0;

    @Column(name = "csv_count", nullable = false)
    @Builder.Default
    private Integer csvCount = 0;

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @Column(name = "total_service_count", nullable = false)
    @Builder.Default
    private Integer totalServiceCount = 0;

    // Execution Counts
    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private Integer successCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Column(name = "in_progress_count", nullable = false)
    @Builder.Default
    private Integer inProgressCount = 0;

    @Column(name = "completed_count", nullable = false)
    @Builder.Default
    private Integer completedCount = 0;

    @Column(name = "expired_count", nullable = false)
    @Builder.Default
    private Integer expiredCount = 0;

    @Column(name = "paused_count", nullable = false)
    @Builder.Default
    private Integer pausedCount = 0;

    // Status: IP, S, F, P, E, C
    @Column(name = "overall_status", nullable = false, length = 8)
    @Builder.Default
    private String overallStatus = "IP";

    // FULL_SUCCESS, PARTIAL_SUCCESS, TOTAL_FAILURE
    @Column(name = "completion_outcome", length = 16)
    private String completionOutcome;

    @Column(name = "request_payload_reference", length = 512)
    private String requestPayloadReference;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;

    @Column(name = "archived_at")
    private Instant archivedAt;
}
