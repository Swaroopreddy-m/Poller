package com.platform.module.transaction.repository;

import com.platform.module.transaction.entity.OutMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutMessageRepository extends JpaRepository<OutMessage, Long> {

    @Query("SELECT m FROM OutMessage m WHERE m.refNo = :refNo")
    Optional<OutMessage> findByRefNo(@Param("refNo") String refNo);

    @Query("SELECT m FROM OutMessage m WHERE m.refNo = :refNo AND m.clientId = :clientId")
    Optional<OutMessage> findByRefNoAndClientId(@Param("refNo") String refNo, @Param("clientId") UUID clientId);

    @Modifying
    @Query("""
        UPDATE OutMessage m 
        SET m.successCount = m.successCount + 1,
            m.inProgressCount = CASE WHEN m.inProgressCount > 0 THEN m.inProgressCount - 1 ELSE 0 END,
            m.completedCount = m.completedCount + 1,
            m.lastUpdatedAt = CURRENT_TIMESTAMP
        WHERE m.refNo = :refNo
    """)
    int recordOperationSuccess(@Param("refNo") String refNo);

    @Modifying
    @Query("""
        UPDATE OutMessage m 
        SET m.failedCount = m.failedCount + 1,
            m.inProgressCount = CASE WHEN m.inProgressCount > 0 THEN m.inProgressCount - 1 ELSE 0 END,
            m.completedCount = m.completedCount + 1,
            m.lastUpdatedAt = CURRENT_TIMESTAMP
        WHERE m.refNo = :refNo
    """)
    int recordOperationFailure(@Param("refNo") String refNo);

    @Modifying
    @Query("""
        UPDATE OutMessage m
        SET m.overallStatus = 'C',
            m.completionOutcome = CASE 
                WHEN m.failedCount = 0 AND m.expiredCount = 0 THEN 'FULL_SUCCESS'
                WHEN m.successCount > 0 THEN 'PARTIAL_SUCCESS'
                ELSE 'TOTAL_FAILURE'
            END,
            m.completedAt = CURRENT_TIMESTAMP,
            m.lastUpdatedAt = CURRENT_TIMESTAMP
        WHERE m.refNo = :refNo
          AND m.totalServiceCount = (m.successCount + m.failedCount + m.expiredCount)
    """)
    int finalizeTransactionIfComplete(@Param("refNo") String refNo);
}
