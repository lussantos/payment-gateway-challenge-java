package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.checkout.payment.gateway.configuration.PaymentProperties;
import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CurrencyValidationServiceTest {

  CurrencyValidationService service = new CurrencyValidationService(
      new PaymentProperties(Set.of("JPY")));

  @Test
  void whenCurrencyIsSupportedDoNothing() {
    assertDoesNotThrow(() -> service.validate("JPY"));
  }

  @Test
  void whenCurrencyIsNotSupportedThrowException() {
    assertThrows(InvalidPaymentRequestException.class, () -> service.validate("USD"));
  }

}
