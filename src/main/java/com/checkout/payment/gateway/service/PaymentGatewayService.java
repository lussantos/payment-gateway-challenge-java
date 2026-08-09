package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.common.StringUtil;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
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

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.debug("Requesting payment processing with object: {}", paymentRequest);
    validateExpiryDate(paymentRequest);
    currencyValidationService.validate(paymentRequest.getCurrency());
    BankPaymentResponse bankResponse = bankSimulatorService.getBankResponse(paymentRequest);
    PostPaymentResponse postPaymentResponse = mapPaymentResponse(paymentRequest);
    validateBankResponseAndSetStatus(postPaymentResponse, bankResponse);
    paymentsRepository.add(postPaymentResponse);
    return postPaymentResponse;
  }

  private void validateExpiryDate(PostPaymentRequest paymentRequest) {
    YearMonth expiryDate = YearMonth.of(
        paymentRequest.getExpiryYear(), paymentRequest.getExpiryMonth());

    if (!expiryDate.isAfter(YearMonth.now())) {
      throw new InvalidPaymentRequestException("Expiry date must be in the future");
    }
  }

  private PostPaymentResponse mapPaymentResponse(PostPaymentRequest paymentRequest) {
    PostPaymentResponse postPaymentResponse = PostPaymentResponse.from(paymentRequest);
    postPaymentResponse.setCardNumberLastFour(
        StringUtil.getLastFourDigits(paymentRequest.getCardNumber()));
    postPaymentResponse.setId(UUID.randomUUID());
    return postPaymentResponse;
  }

  private void validateBankResponseAndSetStatus(PostPaymentResponse postPaymentResponse,
      BankPaymentResponse bankResponse) {
    if (bankResponse.authorized()) {
      postPaymentResponse.setStatus(PaymentStatus.AUTHORIZED);
    } else {
      postPaymentResponse.setStatus(PaymentStatus.DECLINED);
    }
  }
}
