package com.checkout.payment.gateway.model.dto;

import com.checkout.payment.gateway.common.StringUtil;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.Payment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import java.math.BigInteger;
import java.util.Currency;
import java.util.UUID;

@Setter
@Accessors(chain = true)
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class PostPaymentResponse {

  private UUID id;

  private PaymentStatus status;

  @JsonProperty("card_number_last_four")
  private String cardNumberLastFour;

  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @JsonProperty("expiry_year")
  private Integer expiryYear;

  private Currency currency;

  private BigInteger amount;

  public static PostPaymentResponse from(Payment payment, String decryptedCardNumber) {
    return new PostPaymentResponse()
        .setId(payment.getId())
        .setStatus(payment.getStatus())
        .setCardNumberLastFour(StringUtil.getLastFourDigits(decryptedCardNumber))
        .setExpiryMonth(payment.getExpiryMonth())
        .setExpiryYear(payment.getExpiryYear())
        .setCurrency(Currency.getInstance(payment.getCurrency()))
        .setAmount(payment.getAmount());
  }
}
