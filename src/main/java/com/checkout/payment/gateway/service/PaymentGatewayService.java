package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.dto.PostPaymentRequest;
import com.checkout.payment.gateway.model.dto.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.YearMonth;
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
  private final CurrencyValidationService currencyValidationService;
  private final PaymentEncryptionService paymentEncryptionService;

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    Payment payment = paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));
    return mapPaymentResponse(payment);
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.debug("Requesting payment processing with object: {}", paymentRequest);
    validateExpiryDate(paymentRequest);
    currencyValidationService.validate(paymentRequest.getCurrency());
    BankPaymentResponse bankResponse = bankSimulatorService.getBankResponse(paymentRequest);
    Payment payment = mapAndPersistPayment(paymentRequest, bankResponse);
    return mapPaymentResponse(payment);
  }

  private Payment mapAndPersistPayment(PostPaymentRequest paymentRequest, BankPaymentResponse bankResponse) {
    Payment payment = Payment.from(paymentRequest);
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

  private void validateExpiryDate(PostPaymentRequest paymentRequest) {
    YearMonth expiryDate = YearMonth.of(
        paymentRequest.getExpiryYear(), paymentRequest.getExpiryMonth());

    if (!expiryDate.isAfter(YearMonth.now())) {
      throw new InvalidPaymentRequestException("Expiry date must be in the future");
    }
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

  private PostPaymentResponse mapPaymentResponse(Payment payment) {
    String cardNumber = paymentEncryptionService.decrypt(payment.getCardNumber());
    return PostPaymentResponse.from(payment, cardNumber);
  }
}
