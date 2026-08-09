package com.checkout.payment.gateway.model.dto;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigInteger;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class PostPaymentResponseTest {

  @Test
  void createsResponseFieldsFromPaymentRequest() {
    PostPaymentRequest request = new PostPaymentRequest()
        .setCardNumber("2222405343248877")
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency("GBP")
        .setAmount(BigInteger.valueOf(100))
        .setCvv("123");

    PostPaymentResponse response = PostPaymentResponse.from(request);

    assertAll(
        () -> assertNotNull(response.getId()),
        () -> assertNull(response.getStatus()),
        () -> assertEquals("8877", response.getCardNumberLastFour()),
        () -> assertEquals(4, response.getExpiryMonth()),
        () -> assertEquals(2099, response.getExpiryYear()),
        () -> assertEquals(Currency.getInstance("GBP"), response.getCurrency()),
        () -> assertEquals(BigInteger.valueOf(100), response.getAmount()));
  }
}
