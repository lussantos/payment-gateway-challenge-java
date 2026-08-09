package com.checkout.payment.gateway.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.IdempotencyConflictException;
import com.checkout.payment.gateway.exception.PaymentValidationError;
import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.dto.ErrorResponse;
import com.checkout.payment.gateway.model.dto.PostPaymentErrorResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class CommonExceptionHandlerTest {

  private final CommonExceptionHandler exceptionHandler = new CommonExceptionHandler();

  @Test
  void returnsNotFoundWithoutExposingEventProcessingDetails() {
    EventProcessingException exception = new EventProcessingException(
        "Payment 3f76befe-2ee4-4ab5-8140-e711bf46cb71 does not exist");

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(exception);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Page not found", response.getBody().getMessage());
  }

  @Test
  void returnsMalformedRequestForUnreadableJson() {
    HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
        "Invalid JSON", new MockHttpInputMessage(new byte[0]));

    ResponseEntity<PostPaymentErrorResponse> response =
        exceptionHandler.handleUnreadablePaymentRequest(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(PaymentStatus.REJECTED, response.getBody().getStatus());
    assertEquals("Payment request body is malformed", response.getBody().getErrorMessage());
    assertEquals(List.of(), response.getBody().getErrors());
  }

  @Test
  void returnsEveryBusinessValidationError() {
    List<PaymentValidationError> errors = List.of(
        new PaymentValidationError("currency", "Currency is not supported"),
        new PaymentValidationError("expiry_date", "Expiry date must be in the future"));

    ResponseEntity<PostPaymentErrorResponse> response =
        exceptionHandler.handlePaymentValidationException(
            new PaymentValidationException(errors));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(PaymentStatus.REJECTED, response.getBody().getStatus());
    assertEquals("Currency is not supported; Expiry date must be in the future",
        response.getBody().getErrorMessage());
    assertEquals(errors, response.getBody().getErrors());
  }

  @Test
  void convertsJakartaErrorsToSortedDistinctPaymentErrors() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
        new Object(), "paymentRequest");
    bindingResult.addError(new FieldError(
        "paymentRequest", "expiryYear", "Expiry year is invalid"));
    bindingResult.addError(new FieldError(
        "paymentRequest", "cardNumber", "Card number is invalid"));
    bindingResult.addError(new FieldError(
        "paymentRequest", "cardNumber", "Card number is invalid"));
    bindingResult.addError(new ObjectError("paymentRequest", "Request is invalid"));
    bindingResult.addError(new ObjectError("paymentRequest", null));
    MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
        mock(MethodParameter.class), bindingResult);

    ResponseEntity<PostPaymentErrorResponse> response =
        exceptionHandler.handleJakartaValidationException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(List.of(
        new PaymentValidationError("card_number", "Card number is invalid"),
        new PaymentValidationError("expiry_year", "Expiry year is invalid"),
        new PaymentValidationError("paymentRequest", "Request is invalid")),
        response.getBody().getErrors());
    assertEquals(
        "Card number is invalid; Expiry year is invalid; Request is invalid",
        response.getBody().getErrorMessage());
  }

  @Test
  void returnsConflictForIdempotencyKeyReuseWithDifferentRequest() {
    String message = "Idempotency-Key has already been used with a different request";

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleIdempotencyConflict(
        new IdempotencyConflictException(message));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(message, response.getBody().getMessage());
  }

}
