package com.platform.module.retention.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_retention_policy")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataRetentionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "data_category", nullable = false, unique = true, length = 64)
    private String dataCategory;

    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Column(name = "retention_period_days", nullable = false)
    private Integer retentionPeriodDays;

    @Column(name = "retention_anchor_column", nullable = false, length = 64)
    @Builder.Default
    private String retentionAnchorColumn = "occurred_at";

    @Column(name = "archive_before_delete", nullable = false)
    @Builder.Default
    private Boolean archiveBeforeDelete = true;

    @Column(name = "archive_storage", nullable = false, length = 32)
    @Builder.Default
    private String archiveStorage = "SUPABASE_COLD";

    @Column(name = "batch_size", nullable = false)
    @Builder.Default
    private Integer batchSize = 5000;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "last_execution_at")
    private Instant lastExecutionAt;

    @Column(name = "next_execution_at")
    private Instant nextExecutionAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
