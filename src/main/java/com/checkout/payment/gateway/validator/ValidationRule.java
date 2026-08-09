package com.checkout.payment.gateway.validator;


public interface ValidationRule {

  ValidationResult validate(PaymentValidationContext context);
}
