package com.platform.module.client.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_service_config", uniqueConstraints = {
    @UniqueConstraint(name = "uq_client_service", columnNames = {"client_id", "service_type"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientMaster client;

    @Column(name = "service_type", nullable = false, length = 32)
    private String serviceType; // EMAIL, SMS, PDF, EXCEL, CSV, REPORT

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "max_batch_size", nullable = false)
    @Builder.Default
    private Integer maxBatchSize = 100;

    @Column(name = "allowed_file_types", length = 64)
    @Builder.Default
    private String allowedFileTypes = "PDF,XLSX,CSV";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
