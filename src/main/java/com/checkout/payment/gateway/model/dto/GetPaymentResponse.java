package com.checkout.payment.gateway.model.dto;

import com.checkout.payment.gateway.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.UUID;

@Setter
@Getter
@ToString
public class GetPaymentResponse {

  private UUID id;

  private PaymentStatus status;

  private int cardNumberLastFour;

  private int expiryMonth;

  private int expiryYear;

  private String currency;

  private int amount;

}
