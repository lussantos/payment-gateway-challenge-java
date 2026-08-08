package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.configuration.PaymentProperties;
import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import org.springframework.stereotype.Service;

@Service
public class CurrencyValidationService {

  private final PaymentProperties paymentProperties;

  public CurrencyValidationService(PaymentProperties paymentProperties) {
    this.paymentProperties = paymentProperties;
  }

  public void validate(String currency) {
    if (!paymentProperties.supportedCurrencies().contains(currency)) {
      throw new InvalidPaymentRequestException(
          "Currency must be one of: " + paymentProperties.supportedCurrencies());
    }
  }
}
