package com.checkout.payment.gateway.model.dto;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  public PostPaymentErrorResponse(String errorMessage) {
    this.status = PaymentStatus.REJECTED;
    this.errorMessage = errorMessage;
  }

  @JsonCreator
  public PostPaymentErrorResponse(
      @JsonProperty("status") PaymentStatus status,
      @JsonProperty("error_message") String errorMessage) {
    this.status = status;
    this.errorMessage = errorMessage;
  }
}
