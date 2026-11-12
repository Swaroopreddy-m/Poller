package com.platform.module.error.repository;

import com.platform.module.error.entity.OutMessageError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutMessageErrorRepository extends JpaRepository<OutMessageError, Long> {

    @Query("SELECT e FROM OutMessageError e WHERE e.refNo = :refNo ORDER BY e.id ASC")
    List<OutMessageError> findByRefNo(@Param("refNo") String refNo);
}
