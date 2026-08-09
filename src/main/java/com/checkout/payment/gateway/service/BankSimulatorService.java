package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class BankSimulatorService {

  private final BankSimulatorClient bankSimulatorClient;

  public BankPaymentResponse getBankResponse(PaymentRequest paymentRequest) {
    try {
      BankPaymentResponse bankResponse = bankSimulatorClient.processPayment(paymentRequest);
      validateResponse(bankResponse);
      return bankResponse;
    } catch (RestClientException exception) {
      throw new BankIntegrationException(
          "Unable to process payment with the acquiring bank", exception);
    }
  }

  private static void validateResponse(BankPaymentResponse bankResponse) {
    if (bankResponse == null) {
      throw new BankIntegrationException("The acquiring bank returned an empty response");
    }
    if (bankResponse.authorized() == null) {
      throw new BankIntegrationException(
          "The acquiring bank returned an empty authorized field value.");
    }
  }
}
