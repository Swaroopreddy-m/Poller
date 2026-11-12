package com.platform.module.retention.repository;

import com.platform.module.retention.entity.DataRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, UUID> {

    List<DataRetentionPolicy> findByEnabledTrue();
}
