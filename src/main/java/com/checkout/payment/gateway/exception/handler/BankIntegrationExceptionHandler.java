package com.checkout.payment.gateway.exception.handler;

import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.model.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class BankIntegrationExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(
      BankIntegrationExceptionHandler.class);

  @ExceptionHandler(BankIntegrationException.class)
  public ResponseEntity<ErrorResponse> handleBankIntegrationException(
      BankIntegrationException exception) {
    LOG.error("Acquiring bank integration failed", exception);

    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("Unable to process payment with the acquiring bank"));
  }
}
