package com.checkout.payment.gateway.validator.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.configuration.PaymentProperties;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.validator.ValidationResult;
import com.checkout.payment.gateway.validator.PaymentValidationContext;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CurrencyRuleTest {

  private final CurrencyRule currencyRule = new CurrencyRule(
      new PaymentProperties(Set.of("USD", "EUR", "GBP")));

  @Test
  void acceptsSupportedCurrency() {
    ValidationResult result = currencyRule.validate(new PaymentValidationContext(
        "payment-key", new PaymentRequest().setCurrency("GBP")));

    assertTrue(result.isValid());
  }

  @Test
  void rejectsUnsupportedCurrencyWithFieldAndMessage() {
    ValidationResult result = currencyRule.validate(new PaymentValidationContext(
        "payment-key", new PaymentRequest().setCurrency("JPY")));

    assertFalse(result.isValid());
    assertEquals("currency", result.getField());
    assertTrue(result.getErrorMessage().contains("Currency 'JPY' is not supported"));
    assertTrue(result.getErrorMessage().contains("USD"));
    assertTrue(result.getErrorMessage().contains("EUR"));
    assertTrue(result.getErrorMessage().contains("GBP"));
  }
}
