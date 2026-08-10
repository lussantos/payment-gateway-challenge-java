package com.checkout.payment.gateway.validator;

import com.checkout.payment.gateway.model.dto.PaymentRequest;

public record PaymentValidationContext(
    String idempotencyKey,
    PaymentRequest paymentRequest) {
}
