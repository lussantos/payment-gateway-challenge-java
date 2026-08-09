package com.checkout.payment.gateway.model.dto;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.Payment;
import java.math.BigInteger;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostPaymentResponseTest {

  @Test
  void createsResponseFieldsFromPayment() {
    UUID paymentId = UUID.fromString("83173e49-4c3f-4952-9df4-1b2d675a5ba5");
    Payment payment = new Payment()
        .setId(paymentId)
        .setStatus(PaymentStatus.AUTHORIZED)
        .setCardNumber("encrypted-card-number")
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency("GBP")
        .setAmount(BigInteger.valueOf(100))
        .setCvv("encrypted-cvv");

    PostPaymentResponse response = PostPaymentResponse.from(
        payment, "2222405343248877");

    assertAll(
        () -> assertEquals(paymentId, response.getId()),
        () -> assertEquals(PaymentStatus.AUTHORIZED, response.getStatus()),
        () -> assertEquals("8877", response.getCardNumberLastFour()),
        () -> assertEquals(4, response.getExpiryMonth()),
        () -> assertEquals(2099, response.getExpiryYear()),
        () -> assertEquals(Currency.getInstance("GBP"), response.getCurrency()),
        () -> assertEquals(BigInteger.valueOf(100), response.getAmount()));
  }
}
