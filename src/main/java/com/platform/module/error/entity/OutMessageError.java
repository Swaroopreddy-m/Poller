package com.platform.module.error.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "out_message_error")
@IdClass(OutMessageError.OutMessageErrorId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutMessageError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "out_message_id", nullable = false)
    private Long outMessageId;

    @Column(name = "ref_no", nullable = false, length = 36)
    private String refNo;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "service_type", nullable = false, length = 32)
    private String serviceType;

    @Column(name = "operation_id")
    private Long operationId;

    @Column(name = "error_code", nullable = false, length = 64)
    private String errorCode;

    @Column(name = "error_category", nullable = false, length = 32)
    private String errorCategory; // VALIDATION, SECURITY, PROVIDER, SYSTEM, STORAGE, TIMEOUT

    @Column(name = "error_message", nullable = false, length = 1024)
    private String errorMessage;

    @Column(name = "error_details", columnDefinition = "jsonb")
    private String errorDetails;

    @Column(name = "retryable", nullable = false)
    @Builder.Default
    private Boolean retryable = false;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutMessageErrorId implements Serializable {
        private Long id;
        private Instant createdAt;
    }
}
