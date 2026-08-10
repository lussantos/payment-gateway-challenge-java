package com.checkout.payment.gateway.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.Payment;
import jakarta.persistence.EntityManager;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PaymentsRepositoryTest {

  @Autowired
  private PaymentsRepository paymentsRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  void persistsAndRetrievesPayment() {
    Payment expectedPayment = getPayment();

    paymentsRepository.saveAndFlush(expectedPayment);
    entityManager.clear();

    Payment storedPayment = paymentsRepository.findById(expectedPayment.getId()).orElseThrow();
    assertEquals(expectedPayment, storedPayment);
  }

  private static Payment getPayment() {
    return new Payment()
        .setId(UUID.fromString("e9f8ac53-345b-4908-9488-4bb970275529"))
        .setStatus(PaymentStatus.AUTHORIZED)
        .setCardNumber("encrypted-card-number")
        .setExpiryMonth(12)
        .setExpiryYear(2099)
        .setCurrency("GBP")
        .setAmount(BigInteger.valueOf(100))
        .setCvv("encrypted-cvv")
        .setAuthorizationCode(UUID.fromString("f033d23a-7847-4c88-b149-518fe88b55c1"))
        .setCreatedAt(Instant.parse("2026-08-09T10:15:30Z"));
  }
}
