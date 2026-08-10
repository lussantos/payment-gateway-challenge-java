package com.checkout.payment.gateway.validator;

import com.checkout.payment.gateway.exception.PaymentValidationError;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentValidator {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentValidator.class);

  private final List<ValidationRule> validationRules;

  public List<PaymentValidationError> validate(
      String idempotencyKey, PaymentRequest request) {
    List<PaymentValidationError> errors = new ArrayList<>();
    PaymentValidationContext context = new PaymentValidationContext(idempotencyKey, request);

    for (ValidationRule rule : validationRules) {
      ValidationResult result = rule.validate(context);
      if (!result.isValid()) {
        LOG.debug("Validation failed for rule '{}': {}",
            result.getRuleName(), result.getErrorMessage());
        errors.add(new PaymentValidationError(result.getField(), result.getErrorMessage()));
      }
    }

    return List.copyOf(errors);
  }
}
