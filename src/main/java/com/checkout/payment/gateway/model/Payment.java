package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "payments")
public class Payment {

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  private String cardNumber;

  private Integer expiryMonth;

  private Integer expiryYear;

  private String currency;

  private BigInteger amount;

  private String cvv;

  private UUID authorizationCode;

  private Instant createdAt;

  public static Payment from(PaymentRequest paymentRequest) {
    return new Payment()
        .setCardNumber(paymentRequest.getCardNumber())
        .setExpiryMonth(paymentRequest.getExpiryMonth())
        .setExpiryYear(paymentRequest.getExpiryYear())
        .setCurrency(paymentRequest.getCurrency())
        .setAmount(paymentRequest.getAmount())
        .setCvv(paymentRequest.getCvv());
  }
}
