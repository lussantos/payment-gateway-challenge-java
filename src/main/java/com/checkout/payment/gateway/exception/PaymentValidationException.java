package com.checkout.payment.gateway.exception;

import lombok.Getter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PaymentValidationException extends RuntimeException {

  private final List<PaymentValidationError> errors;

  public PaymentValidationException(List<PaymentValidationError> errors) {
    super(errors.stream()
        .map(PaymentValidationError::message)
        .collect(Collectors.joining("; ")));
    this.errors = List.copyOf(errors);
  }

}
