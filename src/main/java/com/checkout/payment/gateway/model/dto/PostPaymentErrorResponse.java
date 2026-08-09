package com.checkout.payment.gateway.model.dto;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentValidationError;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class PostPaymentErrorResponse {

  private final PaymentStatus status;

  @JsonProperty("error_message")
  private final String errorMessage;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private final List<PaymentValidationError> errors;

  public PostPaymentErrorResponse(String errorMessage) {
    this(PaymentStatus.REJECTED, errorMessage, List.of());
  }

  public PostPaymentErrorResponse(
      String errorMessage, List<PaymentValidationError> errors) {
    this(PaymentStatus.REJECTED, errorMessage, errors);
  }

  @JsonCreator
  public PostPaymentErrorResponse(
      @JsonProperty("status") PaymentStatus status,
      @JsonProperty("error_message") String errorMessage,
      @JsonProperty("errors") List<PaymentValidationError> errors) {
    this.status = status;
    this.errorMessage = errorMessage;
    this.errors = errors == null ? List.of() : List.copyOf(errors);
  }
}
