package com.checkout.payment.gateway.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.checkout.payment.gateway.model.dto.PaymentRequest;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class PaymentTest {

  @Test
  void createsPaymentFromRequest() {
    PaymentRequest request = new PaymentRequest()
        .setCardNumber("2222405343248877")
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency("GBP")
        .setAmount(BigInteger.valueOf(100))
        .setCvv("123");

    Payment payment = Payment.from(request);

    assertAll(
        () -> assertNull(payment.getId()),
        () -> assertEquals(request.getCardNumber(), payment.getCardNumber()),
        () -> assertEquals(request.getExpiryMonth(), payment.getExpiryMonth()),
        () -> assertEquals(request.getExpiryYear(), payment.getExpiryYear()),
        () -> assertEquals(request.getCurrency(), payment.getCurrency()),
        () -> assertEquals(request.getAmount(), payment.getAmount()),
        () -> assertEquals(request.getCvv(), payment.getCvv()),
        () -> assertNull(payment.getCreatedAt()));
  }
}
