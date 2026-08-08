package com.checkout.payment.gateway.exception.handler;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import com.checkout.payment.gateway.model.dto.ErrorResponse;
import com.checkout.payment.gateway.model.dto.PostPaymentErrorResponse;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse("Page not found"),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<PostPaymentErrorResponse> handleInvalidPaymentRequest(
      MethodArgumentNotValidException ex) {
    String errorMessage = ex.getBindingResult().getAllErrors().stream()
        .map(DefaultMessageSourceResolvable::getDefaultMessage)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .collect(Collectors.joining("; "));

    LOG.debug("Payment request rejected: {}", errorMessage);

    return ResponseEntity.badRequest()
        .body(new PostPaymentErrorResponse(errorMessage));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<PostPaymentErrorResponse> handleUnreadablePaymentRequest(
      HttpMessageNotReadableException ex) {
    LOG.debug("Payment request rejected because its body could not be read", ex);

    return ResponseEntity.badRequest()
        .body(new PostPaymentErrorResponse("Payment request body is malformed"));
  }

  @ExceptionHandler(InvalidPaymentRequestException.class)
  public ResponseEntity<PostPaymentErrorResponse> handleInvalidPaymentRequest(
      InvalidPaymentRequestException ex) {
    LOG.debug("Payment request rejected: {}", ex.getMessage());

    return ResponseEntity.badRequest()
        .body(new PostPaymentErrorResponse(ex.getMessage()));
  }
}
