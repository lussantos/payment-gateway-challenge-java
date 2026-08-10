package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.client.dto.BankPaymentRequest;
import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.configuration.BankSimulatorProperties;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BankSimulatorClient {

  private final RestTemplate restTemplate;
  private final BankSimulatorProperties bankSimulatorProperties;

  public BankSimulatorClient(
      RestTemplate restTemplate,
      BankSimulatorProperties bankSimulatorProperties) {
    this.restTemplate = restTemplate;
    this.bankSimulatorProperties = bankSimulatorProperties;
  }

  public BankPaymentResponse processPayment(PaymentRequest paymentRequest) {
    return restTemplate.postForObject(
        bankSimulatorProperties.paymentsUrl(),
        BankPaymentRequest.from(paymentRequest),
        BankPaymentResponse.class);
  }
}
