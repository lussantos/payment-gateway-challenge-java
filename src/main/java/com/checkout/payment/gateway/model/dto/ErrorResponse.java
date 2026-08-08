package com.checkout.payment.gateway.model.dto;

import lombok.Getter;

@Getter
public class ErrorResponse {
  private final String message;

  public ErrorResponse(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "ErrorResponse{" +
        "message='" + message + '\'' +
        '}';
  }
}
