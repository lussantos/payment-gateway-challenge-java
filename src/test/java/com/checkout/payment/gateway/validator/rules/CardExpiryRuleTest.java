package com.checkout.payment.gateway.validator.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.validator.ValidationResult;
import org.junit.jupiter.api.Test;

class CardExpiryRuleTest {

  private final CardExpiryRule cardExpiryRule = new CardExpiryRule();

  @Test
  void acceptsFutureExpiryDate() {
    PaymentRequest paymentRequest = new PaymentRequest()
        .setExpiryMonth(12)
        .setExpiryYear(2099);

    ValidationResult result = cardExpiryRule.validate(paymentRequest);

    assertTrue(result.isValid());
  }

  @Test
  void rejectsExpiredDateWithFieldAndMessage() {
    PaymentRequest paymentRequest = new PaymentRequest()
        .setExpiryMonth(1)
        .setExpiryYear(2020);

    ValidationResult result = cardExpiryRule.validate(paymentRequest);

    assertFalse(result.isValid());
    assertEquals("expiry_date", result.getField());
    assertEquals("Expiry date must be in the future", result.getErrorMessage());
  }
}
