package com.checkout.payment.gateway.validator;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ValidationResult {

  private final boolean valid;
  private final String errorMessage;
  private final String ruleName;
  private final String field;

  private ValidationResult(boolean valid, String errorMessage, String ruleName, String field) {
    this.valid = valid;
    this.errorMessage = errorMessage;
    this.ruleName = ruleName;
    this.field = field;
  }

  public static ValidationResult success(String ruleName) {
    return new ValidationResult(true, null, ruleName, null);
  }

  public static ValidationResult failure(String ruleName, String field, String errorMessage) {
    return new ValidationResult(false, errorMessage, ruleName, field);
  }

}
