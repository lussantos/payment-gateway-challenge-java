package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;

class PaymentEncryptionServiceTest {

  private final PaymentEncryptionService paymentEncryptionService =
      new PaymentEncryptionService(
          Encryptors.delux("test-payment-encryption-secret", "5c0744940b5c369b"));

  @Test
  void encryptsAndDecryptsSensitivePaymentData() {
    String cardNumber = "2222405343248877";
    String cvv = "123";

    String encryptedCardNumber = paymentEncryptionService.encrypt(cardNumber);
    String encryptedCvv = paymentEncryptionService.encrypt(cvv);

    assertNotEquals(cardNumber, encryptedCardNumber);
    assertNotEquals(cvv, encryptedCvv);
    assertEquals(cardNumber, paymentEncryptionService.decrypt(encryptedCardNumber));
    assertEquals(cvv, paymentEncryptionService.decrypt(encryptedCvv));
  }
}
