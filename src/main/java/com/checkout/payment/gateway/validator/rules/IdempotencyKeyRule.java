package com.checkout.payment.gateway.validator.rules;

import com.checkout.payment.gateway.validator.PaymentValidationContext;
import com.checkout.payment.gateway.validator.ValidationResult;
import com.checkout.payment.gateway.validator.ValidationRule;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyRule implements ValidationRule {

  public static final int MAX_KEY_LENGTH = 255;
  private static final String RULE_NAME = "IdempotencyKeyRule";
  private static final String FIELD = "idempotency_key";
  private static final String ERROR_MESSAGE =
      "Idempotency-Key must contain between 1 and 255 characters";

  @Override
  public ValidationResult validate(PaymentValidationContext context) {
    String idempotencyKey = context.idempotencyKey();
    if (idempotencyKey == null
        || idempotencyKey.isBlank()
        || idempotencyKey.length() > MAX_KEY_LENGTH) {
      return ValidationResult.failure(RULE_NAME, FIELD, ERROR_MESSAGE);
    }
    return ValidationResult.success(RULE_NAME);
  }
}
