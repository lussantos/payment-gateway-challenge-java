package com.checkout.payment.gateway.exception;

public class InvalidPaymentRequestException extends RuntimeException {

  public InvalidPaymentRequestException(String message) {
    super(message);
  }
}
