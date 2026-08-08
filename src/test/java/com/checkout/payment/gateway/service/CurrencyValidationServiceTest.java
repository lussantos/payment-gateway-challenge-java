package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.checkout.payment.gateway.configuration.PaymentProperties;
import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CurrencyValidationServiceTest {

  @Test
  void validatesAgainstTheConfiguredCurrencies() {
    CurrencyValidationService service = new CurrencyValidationService(
        new PaymentProperties(Set.of("JPY")));

    assertDoesNotThrow(() -> service.validate("JPY"));
    assertThrows(InvalidPaymentRequestException.class, () -> service.validate("USD"));
  }
}
