package com.platform.module.retention.job;

import com.platform.module.retention.entity.DataRetentionPolicy;
import com.platform.module.retention.repository.DataRetentionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Enterprise Configurable Data Retention and Archival Job.
 * Executes safe, throttled batch deletions avoiding lock escalation and WAL spikes.
 * Never deletes active or in-progress transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetentionCleanupJob {

    private final DataRetentionPolicyRepository policyRepository;
    private final JdbcTemplate jdbcTemplate;

    // Run daily at 02:00 AM UTC
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeRetentionPolicies() {
        log.info("Starting scheduled retention policy cleanup...");
        List<DataRetentionPolicy> policies = policyRepository.findByEnabledTrue();

        for (DataRetentionPolicy policy : policies) {
            executePolicy(policy);
        }
        log.info("Completed scheduled retention policy cleanup.");
    }

    private void executePolicy(DataRetentionPolicy policy) {
        log.info("Executing retention policy: {} for table: {}", policy.getDataCategory(), policy.getTableName());
        String tableName = policy.getTableName();
        int batchSize = policy.getBatchSize();
        long totalDeleted = 0;

        try {
            while (true) {
                // Key-Set Cursor non-blocking batch delete query
                String sql;
                if ("out_message".equalsIgnoreCase(tableName)) {
                    // Critical safety check: NEVER delete IP (In Progress) or P (Paused)
                    sql = """
                        DELETE FROM out_message
                        WHERE id IN (
                            SELECT id FROM out_message
                            WHERE retention_until < CURRENT_TIMESTAMP
                              AND overall_status IN ('C', 'E')
                            ORDER BY id ASC
                            LIMIT ?
                        )
                    """;
                } else {
                    sql = String.format("""
                        DELETE FROM %s
                        WHERE id IN (
                            SELECT id FROM %s
                            WHERE retention_until < CURRENT_TIMESTAMP
                            ORDER BY id ASC
                            LIMIT ?
                        )
                    """, tableName, tableName);
                }

                int deleted = jdbcTemplate.update(sql, batchSize);
                totalDeleted += deleted;

                if (deleted < batchSize) {
                    break;
                }

                // Pause briefly (50ms) to allow DB vacuum, WAL checkpointing and avoid lock starvation
                Thread.sleep(50);
            }

            policy.setLastExecutionAt(Instant.now());
            policyRepository.save(policy);
            log.info("Policy {} finished: successfully purged {} expired records.", policy.getDataCategory(), totalDeleted);

        } catch (Exception ex) {
            log.error("Failed executing retention cleanup for table: {}", tableName, ex);
        }
    }
}
