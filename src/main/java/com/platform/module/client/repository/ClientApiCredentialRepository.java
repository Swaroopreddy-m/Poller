package com.platform.module.client.repository;

import com.platform.module.client.entity.ClientApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientApiCredentialRepository extends JpaRepository<ClientApiCredential, UUID> {

    @Query("SELECT c FROM ClientApiCredential c JOIN FETCH c.client WHERE c.apiKeyPrefix = :prefix AND c.status = 'ACTIVE'")
    Optional<ClientApiCredential> findActiveByPrefixWithClient(@Param("prefix") String prefix);
}
