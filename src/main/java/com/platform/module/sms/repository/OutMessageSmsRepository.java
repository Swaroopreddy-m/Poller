package com.platform.module.sms.repository;

import com.platform.module.sms.entity.OutMessageSms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutMessageSmsRepository extends JpaRepository<OutMessageSms, OutMessageSms.OutMessageSmsId> {
    List<OutMessageSms> findByRefNo(String refNo);
}
