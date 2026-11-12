package com.platform.module.email.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "out_message_email")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutMessageEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "out_message_id", nullable = false)
    private Long outMessageId;

    @Column(name = "ref_no", nullable = false, length = 36)
    private String refNo;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "cc_list", columnDefinition = "TEXT")
    private String ccList;

    @Column(name = "bcc_list", columnDefinition = "TEXT")
    private String bccList;

    @Column(name = "subject", nullable = false, length = 512)
    private String subject;

    @Column(name = "rendered_body", nullable = false, columnDefinition = "TEXT")
    private String renderedBody;

    @Column(name = "is_html", nullable = false)
    @Builder.Default
    private Boolean isHtml = true;

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

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;
}
