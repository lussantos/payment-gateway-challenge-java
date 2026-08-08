package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.YearMonth;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  @NotBlank(message = "Card number is required")
  @Pattern(regexp = "\\d{14,19}",
      message = "Card number must contain between 14 and 19 digits")
  private String cardNumber;

  @JsonProperty("expiry_month")
  @NotNull(message = "Expiry month is required")
  @Min(value = 1, message = "Expiry month value must be from 1 to 12")
  @Max(value = 12, message = "Expiry month value must be from 1 to 12")
  private Integer expiryMonth;

  @JsonProperty("expiry_year")
  @NotNull(message = "Expiry year is required")
  private Integer expiryYear;

  @NotBlank(message = "Currency is required")
  @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase code")
  private String currency;

  @NotNull(message = "Amount is required")
  private BigInteger amount;

  @NotBlank(message = "CVV is required")
  @Pattern(regexp = "\\d{3,4}", message = "CVV must contain 3 or 4 digits")
  private String cvv;

  @JsonIgnore
  @AssertTrue(message = "Expiry date must be in the future")
  public boolean isExpiryDateInFuture() {
    if (expiryMonth == null || expiryYear == null || expiryMonth < 1 || expiryMonth > 12) {
      return true;
    }
    //TODO ideally date reference should have specific logic to consider request location in a distributed env
    return YearMonth.of(expiryYear, expiryMonth).isAfter(YearMonth.now());
  }

  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

}
