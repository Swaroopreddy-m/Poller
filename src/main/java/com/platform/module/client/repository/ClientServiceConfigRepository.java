package com.platform.module.client.repository;

import com.platform.module.client.entity.ClientServiceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientServiceConfigRepository extends JpaRepository<ClientServiceConfig, UUID> {

    Optional<ClientServiceConfig> findByClientIdAndServiceType(UUID clientId, String serviceType);
}
