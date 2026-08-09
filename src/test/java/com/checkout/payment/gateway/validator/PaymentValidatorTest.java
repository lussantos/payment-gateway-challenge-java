package com.checkout.payment.gateway.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.checkout.payment.gateway.exception.PaymentValidationError;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentValidatorTest {

  @Test
  void returnsEveryValidationError() {
    PaymentRequest paymentRequest = new PaymentRequest();
    ValidationRule expiryRule = request -> ValidationResult.failure(
        "CardExpiryRule", "expiry_date", "Expiry date must be in the future");
    ValidationRule currencyRule = request -> ValidationResult.failure(
        "CurrencyRule", "currency", "Currency is not supported");
    PaymentValidator paymentValidator = new PaymentValidator(
        List.of(expiryRule, currencyRule));

    List<PaymentValidationError> errors = paymentValidator.validate(paymentRequest);

    assertEquals(List.of(
        new PaymentValidationError("expiry_date", "Expiry date must be in the future"),
        new PaymentValidationError("currency", "Currency is not supported")), errors);
  }

  @Test
  void returnsEmptyListWhenEveryRulePasses() {
    PaymentRequest paymentRequest = new PaymentRequest();
    ValidationRule successfulRule = request -> ValidationResult.success("SuccessfulRule");
    PaymentValidator paymentValidator = new PaymentValidator(List.of(successfulRule));

    List<PaymentValidationError> errors = paymentValidator.validate(paymentRequest);

    assertEquals(List.of(), errors);
  }
}
