package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select record from IdempotencyRecord record "
      + "where record.idempotencyKey = :idempotencyKey")
  IdempotencyRecord lockById(@Param("idempotencyKey") String idempotencyKey);
}
