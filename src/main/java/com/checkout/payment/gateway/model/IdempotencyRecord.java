package com.checkout.payment.gateway.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

  @Id
  private String idempotencyKey;

  private String requestHash;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "payment_id", unique = true)
  private Payment payment;

  private Instant createdAt;

  public static IdempotencyRecord pending(String idempotencyKey, String requestHash) {
    return new IdempotencyRecord()
        .setIdempotencyKey(idempotencyKey)
        .setRequestHash(requestHash)
        .setCreatedAt(Instant.now());
  }
}
