package com.checkout.payment.gateway.validator.rules;

import java.time.YearMonth;

import com.checkout.payment.gateway.validator.PaymentValidationContext;
import com.checkout.payment.gateway.validator.ValidationResult;
import com.checkout.payment.gateway.validator.ValidationRule;
import org.springframework.stereotype.Component;

@Component
public class CardExpiryRule implements ValidationRule {

  private static final String RULE_NAME = "CardExpiryRule";

  @Override
  public ValidationResult validate(PaymentValidationContext context) {
    var request = context.paymentRequest();
    YearMonth expiryDate = YearMonth.of(
        request.getExpiryYear(), request.getExpiryMonth());

    if (!expiryDate.isAfter(YearMonth.now())) {
      return ValidationResult.failure(
          RULE_NAME, "expiry_date", "Expiry date must be in the future");
    }
    return ValidationResult.success(RULE_NAME);
  }
}
