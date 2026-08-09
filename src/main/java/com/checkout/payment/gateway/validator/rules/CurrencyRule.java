package com.checkout.payment.gateway.validator.rules;

import com.checkout.payment.gateway.configuration.PaymentProperties;
import com.checkout.payment.gateway.validator.PaymentValidationContext;
import com.checkout.payment.gateway.validator.ValidationResult;
import com.checkout.payment.gateway.validator.ValidationRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrencyRule implements ValidationRule {

  private static final String RULE_NAME = "CurrencyRule";

  private final PaymentProperties paymentProperties;

  @Override
  public ValidationResult validate(PaymentValidationContext context) {
    String currency = context.paymentRequest().getCurrency();

    if (!paymentProperties.supportedCurrencies().contains(currency)) {
      return ValidationResult.failure(
          RULE_NAME,
          "currency",
          "Currency '" + currency + "' is not supported. Supported currencies: "
              + paymentProperties.supportedCurrencies());
    }

    return ValidationResult.success(RULE_NAME);
  }
}
