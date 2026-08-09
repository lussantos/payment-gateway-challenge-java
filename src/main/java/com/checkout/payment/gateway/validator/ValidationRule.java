package com.checkout.payment.gateway.validator;


import com.checkout.payment.gateway.model.dto.PaymentRequest;

public interface ValidationRule {

  ValidationResult validate(PaymentRequest request);
}
