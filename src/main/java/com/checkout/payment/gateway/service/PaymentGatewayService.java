package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.dto.PostPaymentRequest;
import com.checkout.payment.gateway.model.dto.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.math.BigInteger;
import java.util.Currency;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final CurrencyValidationService currencyValidationService;

  public PaymentGatewayService(
      PaymentsRepository paymentsRepository,
      CurrencyValidationService currencyValidationService) {
    this.paymentsRepository = paymentsRepository;
    this.currencyValidationService = currencyValidationService;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }


  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.debug("Requesting payment processing with object: {}", paymentRequest);
    currencyValidationService.validate(paymentRequest.getCurrency());

    return new PostPaymentResponse().setId(UUID.fromString("bede9e5d-d54f-4e99-b73b-a30941cc9048"))
        .setAmount(BigInteger.valueOf(10))
        .setCurrency(Currency.getInstance("USD"))
        .setStatus(PaymentStatus.AUTHORIZED)
        .setExpiryMonth(12)
        .setExpiryYear(2024)
        .setCardNumberLastFour(4321);
  }
}
