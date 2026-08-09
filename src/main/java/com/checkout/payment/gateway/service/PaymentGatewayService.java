package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.PaymentValidationError;
import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.model.dto.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validator.PaymentValidator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankSimulatorService bankSimulatorService;
  private final PaymentEncryptionService paymentEncryptionService;
  private final PaymentValidator paymentValidator;

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    Payment payment = paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));
    return mapPaymentResponse(payment);
  }

  public PaymentResponse processPayment(PaymentRequest paymentRequest) {
    LOG.debug("Requesting payment processing with object: {}", paymentRequest);
    validatePaymentRequest(paymentRequest);
    BankPaymentResponse bankResponse = bankSimulatorService.processBankPayment(paymentRequest);
    Payment payment = mapAndPersistPayment(paymentRequest, bankResponse);
    return mapPaymentResponse(payment);
  }

  private void validatePaymentRequest(PaymentRequest paymentRequest) {
    List<PaymentValidationError> errors = paymentValidator.validate(paymentRequest);
    if (!errors.isEmpty()) {
      throw new PaymentValidationException(errors);
    }
  }

  private Payment mapAndPersistPayment(PaymentRequest paymentRequest, BankPaymentResponse bankResponse) {
    Payment payment = Payment.from(paymentRequest)
        .setId(UUID.randomUUID())
        .setCreated(Instant.now());
    applyBankResponse(payment, bankResponse);
    encryptSensitiveData(payment);
    paymentsRepository.add(payment);
    return payment;
  }

  private void encryptSensitiveData(Payment payment) {
    payment
        .setCardNumber(paymentEncryptionService.encrypt(payment.getCardNumber()))
        .setCvv(paymentEncryptionService.encrypt(payment.getCvv()));
  }

  private void applyBankResponse(Payment payment,
      BankPaymentResponse bankResponse) {
    if (bankResponse.authorized()) {
      payment.setStatus(PaymentStatus.AUTHORIZED);
      if (bankResponse.authorizationCode() != null) {
        payment.setAuthorizationCode(UUID.fromString(bankResponse.authorizationCode()));
      }
    } else {
      payment.setStatus(PaymentStatus.DECLINED);
    }
  }

  private PaymentResponse mapPaymentResponse(Payment payment) {
    String cardNumber = paymentEncryptionService.decrypt(payment.getCardNumber());
    return PaymentResponse.from(payment, cardNumber);
  }
}
