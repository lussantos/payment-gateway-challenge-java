package com.checkout.payment.gateway.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.model.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BankIntegrationExceptionHandlerTest {

  private final BankIntegrationExceptionHandler exceptionHandler =
      new BankIntegrationExceptionHandler();

  @Test
  void returnsBadGatewayWithoutExposingIntegrationDetails() {
    BankIntegrationException exception = new BankIntegrationException(
        "Connection refused: http://bank-simulator:8080/payments");

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleBankIntegrationException(exception);

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(
        "Unable to process payment with the acquiring bank",
        response.getBody().getMessage());
  }
}
