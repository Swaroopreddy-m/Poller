package com.platform.module.transaction.repository;

import com.platform.module.transaction.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByClientIdAndIdempotencyKey(UUID clientId, String idempotencyKey);
}
