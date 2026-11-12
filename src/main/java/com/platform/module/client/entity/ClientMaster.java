package com.platform.module.client.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_master")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_code", nullable = false, unique = true, length = 32)
    private String clientCode;

    @Column(name = "client_name", nullable = false, length = 128)
    private String clientName;

    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "tier", nullable = false, length = 16)
    @Builder.Default
    private String tier = "STANDARD";

    @Column(name = "max_rate_limit_per_min", nullable = false)
    @Builder.Default
    private Integer maxRateLimitPerMin = 600;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
