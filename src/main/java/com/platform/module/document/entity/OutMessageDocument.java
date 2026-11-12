package com.platform.module.document.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "out_message_document")
@IdClass(OutMessageDocument.OutMessageDocumentId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutMessageDocument {

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

    @Column(name = "document_type", nullable = false, length = 16)
    private String documentType; // PDF, EXCEL, CSV, STATEMENT

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_bucket", nullable = false, length = 128)
    private String storageBucket;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "mime_type", nullable = false, length = 64)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    // Status: IP, S, F, E
    @Column(name = "status", nullable = false, length = 8)
    @Builder.Default
    private String status = "IP";

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutMessageDocumentId implements Serializable {
        private Long id;
        private Instant createdAt;
    }
}
