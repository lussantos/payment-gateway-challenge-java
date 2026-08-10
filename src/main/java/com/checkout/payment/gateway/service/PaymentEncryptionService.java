package com.checkout.payment.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEncryptionService {

  private final TextEncryptor textEncryptor;

  public String encrypt(String value) {
    return textEncryptor.encrypt(value);
  }

  public String decrypt(String encryptedValue) {
    return textEncryptor.decrypt(encryptedValue);
  }
}
