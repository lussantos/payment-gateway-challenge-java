package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.IdempotencyRecord;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class IdempotencyRecordCreator {

  private final EntityManager entityManager;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void create(String idempotencyKey, String requestHash) {
    entityManager.persist(IdempotencyRecord.pending(idempotencyKey, requestHash));
    entityManager.flush();
  }
}
