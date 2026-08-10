package com.checkout.payment.gateway.client.dto;

import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;

public record BankPaymentRequest(
    @JsonProperty("card_number")
    String cardNumber,
    @JsonProperty("expiry_date")
    String expiryDate,
    String currency,
    BigInteger amount,
    String cvv) {

  public static BankPaymentRequest from(PaymentRequest paymentRequest) {
    String expiryDate = String.format(
        "%02d/%04d", paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear());

    return new BankPaymentRequest(
        paymentRequest.getCardNumber(),
        expiryDate,
        paymentRequest.getCurrency(),
        paymentRequest.getAmount(),
        paymentRequest.getCvv());
  }
}
