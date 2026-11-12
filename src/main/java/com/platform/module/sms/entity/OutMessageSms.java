package com.platform.module.sms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "out_message_sms")
@IdClass(OutMessageSms.OutMessageSmsId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutMessageSms {

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

    @Column(name = "recipient_phone", nullable = false, length = 32)
    private String recipientPhone;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "rendered_content", nullable = false, columnDefinition = "TEXT")
    private String renderedContent;

    @Column(name = "provider_name", length = 64)
    private String providerName;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    // Status: IP, S, F, P, E
    @Column(name = "status", nullable = false, length = 8)
    @Builder.Default
    private String status = "IP";

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutMessageSmsId implements Serializable {
        private Long id;
        private Instant createdAt;
    }
}
