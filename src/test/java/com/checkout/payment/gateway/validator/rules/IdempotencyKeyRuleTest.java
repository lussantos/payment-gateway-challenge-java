package com.checkout.payment.gateway.validator.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.validator.PaymentValidationContext;
import com.checkout.payment.gateway.validator.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class IdempotencyKeyRuleTest {

  private final IdempotencyKeyRule rule = new IdempotencyKeyRule();

  @Test
  void acceptsKeysWithinTheAllowedLength() {
    assertTrue(validate("merchant-payment-123").isValid());
    assertTrue(validate("a".repeat(255)).isValid());
  }

  @ParameterizedTest
  @MethodSource("invalidKeys")
  void rejectsMissingBlankAndOversizedKeys(String key) {
    ValidationResult result = validate(key);

    assertEquals("Idempotency-Key must contain between 1 and 255 characters",
        result.getErrorMessage());
    assertEquals("idempotency_key", result.getField());
  }

  private static Stream<String> invalidKeys() {
    return Stream.of(null, "", "   ", "a".repeat(256));
  }

  private ValidationResult validate(String key) {
    return rule.validate(new PaymentValidationContext(key, new PaymentRequest()));
  }
}
