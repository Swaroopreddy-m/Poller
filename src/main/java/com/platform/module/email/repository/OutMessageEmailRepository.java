package com.platform.module.email.repository;

import com.platform.module.email.entity.OutMessageEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutMessageEmailRepository extends JpaRepository<OutMessageEmail, OutMessageEmail.OutMessageEmailId> {
    List<OutMessageEmail> findByRefNo(String refNo);
}
