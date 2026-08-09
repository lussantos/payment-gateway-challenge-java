package com.checkout.payment.gateway.exception.handler;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.PaymentValidationError;
import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.dto.ErrorResponse;
import com.checkout.payment.gateway.model.dto.PostPaymentErrorResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
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
  public ResponseEntity<PostPaymentErrorResponse> handleJakartaValidationException(
      MethodArgumentNotValidException ex) {
    return createRejectedResponse(extractValidationErrors(ex));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<PostPaymentErrorResponse> handleUnreadablePaymentRequest(
      HttpMessageNotReadableException ex) {
    LOG.debug("Payment request rejected because its body could not be read", ex);

    return ResponseEntity.badRequest()
        .body(new PostPaymentErrorResponse("Payment request body is malformed"));
  }

  @ExceptionHandler(PaymentValidationException.class)
  public ResponseEntity<PostPaymentErrorResponse> handlePaymentValidationException(
      PaymentValidationException ex) {
    return createRejectedResponse(ex.getErrors());
  }

  private static List<PaymentValidationError> extractValidationErrors(
      MethodArgumentNotValidException exception) {
    return exception.getBindingResult().getAllErrors().stream()
        .filter(error -> error.getDefaultMessage() != null)
        .map(CommonExceptionHandler::toValidationError)
        .distinct()
        .sorted(Comparator.comparing(PaymentValidationError::field)
            .thenComparing(PaymentValidationError::message))
        .toList();
  }

  private static PaymentValidationError toValidationError(ObjectError error) {
    String field = error instanceof FieldError fieldError
        ? toSnakeCase(fieldError.getField())
        : error.getObjectName();
    return new PaymentValidationError(
        field, Objects.requireNonNull(error.getDefaultMessage()));
  }

  private static ResponseEntity<PostPaymentErrorResponse> createRejectedResponse(
      List<PaymentValidationError> errors) {
    String errorMessage = errors.stream()
        .map(PaymentValidationError::message)
        .collect(Collectors.joining("; "));
    LOG.debug("Payment request rejected: {}", errorMessage);

    return ResponseEntity.badRequest()
        .body(new PostPaymentErrorResponse(errorMessage, errors));
  }

  private static String toSnakeCase(String field) {
    return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }

}
