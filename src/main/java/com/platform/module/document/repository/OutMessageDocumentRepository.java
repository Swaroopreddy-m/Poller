package com.platform.module.document.repository;

import com.platform.module.document.entity.OutMessageDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutMessageDocumentRepository extends JpaRepository<OutMessageDocument, OutMessageDocument.OutMessageDocumentId> {
    List<OutMessageDocument> findByRefNo(String refNo);
}
