package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.dto.PaymentRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestFingerprint {

  public String hash(PaymentRequest request) {
    String canonicalRequest = String.join("\n",
        field(request.getCardNumber()),
        field(request.getExpiryMonth()),
        field(request.getExpiryYear()),
        field(request.getCurrency()),
        field(request.getAmount()),
        field(request.getCvv()));
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256")
              .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  private static String field(Object value) {
    String text = String.valueOf(value);
    return text.length() + ":" + text;
  }
}
