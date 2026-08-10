package com.checkout.payment.gateway.exception;

public record PaymentValidationError(String field, String message) {
}
