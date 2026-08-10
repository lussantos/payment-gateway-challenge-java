package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.dto.BankPaymentRequest;
import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.configuration.BankSimulatorProperties;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class BankSimulatorClientTest {

  @MockBean
  private RestTemplate restTemplate;
  @Autowired
  private BankSimulatorProperties bankSimulatorProperties;
  @Autowired
  private BankSimulatorClient bankSimulatorClient;

  @Test
  void sendsPaymentToSimulatorAndReturnsAuthorizationResult() {

    BankPaymentRequest expectedBankRequest = getBankPaymentRequest();

    BankPaymentResponse expectedBankResponse = new BankPaymentResponse(
        true, "0bb07405-6d44-4b50-a14f-7ae0beff13ad");

    when(restTemplate.postForObject(
        bankSimulatorProperties.paymentsUrl(), expectedBankRequest, BankPaymentResponse.class))
        .thenReturn(expectedBankResponse);

    BankPaymentResponse response = bankSimulatorClient.processPayment(getPaymentRequest());

    assertEquals(expectedBankResponse, response);
    verify(restTemplate).postForObject(
        bankSimulatorProperties.paymentsUrl(), expectedBankRequest, BankPaymentResponse.class);
    verifyNoMoreInteractions(restTemplate);
  }

  private static BankPaymentRequest getBankPaymentRequest() {
    return new BankPaymentRequest(
        "2222405343248877",
        "04/2099",
        "GBP",
        BigInteger.valueOf(100),
        "123");
  }

  private static PaymentRequest getPaymentRequest() {
    return new PaymentRequest()
        .setCardNumber("2222405343248877")
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency("GBP")
        .setAmount(BigInteger.valueOf(100))
        .setCvv("123");
  }
}
