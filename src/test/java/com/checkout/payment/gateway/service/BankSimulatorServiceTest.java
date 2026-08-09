package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.model.dto.PostPaymentRequest;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class BankSimulatorServiceTest {

  @Mock
  private BankSimulatorClient bankSimulatorClient;

  @InjectMocks
  private BankSimulatorService bankSimulatorService;

  @Test
  void returnsBankResponse() {
    PostPaymentRequest paymentRequest = getPaymentRequest();
    BankPaymentResponse expectedResponse = new BankPaymentResponse(true, "authorization-code");
    when(bankSimulatorClient.processPayment(paymentRequest)).thenReturn(expectedResponse);
    BankPaymentResponse response = bankSimulatorService.getBankResponse(paymentRequest);

    assertEquals(expectedResponse, response);
  }

  @Test
  void translatesRestClientFailureAndPreservesCause() {
    PostPaymentRequest paymentRequest = getPaymentRequest();
    ResourceAccessException clientException = new ResourceAccessException("Connection refused");
    when(bankSimulatorClient.processPayment(paymentRequest)).thenThrow(clientException);

    BankIntegrationException exception = assertThrows(
        BankIntegrationException.class,
        () -> bankSimulatorService.getBankResponse(paymentRequest));

    assertEquals("Unable to process payment with the acquiring bank", exception.getMessage());
    assertSame(clientException, exception.getCause());
  }

  @Test
  void rejectsEmptyBankResponse() {
    PostPaymentRequest paymentRequest = getPaymentRequest();
    when(bankSimulatorClient.processPayment(paymentRequest)).thenReturn(null);

    BankIntegrationException exception = assertThrows(
        BankIntegrationException.class,
        () -> bankSimulatorService.getBankResponse(paymentRequest));

    assertEquals("The acquiring bank returned an empty response", exception.getMessage());
  }

  @Test
  void rejectsEmptyAuthorizedInBankResponse() {
    PostPaymentRequest paymentRequest = getPaymentRequest();
    when(bankSimulatorClient.processPayment(paymentRequest)).thenReturn(new BankPaymentResponse(null, "authorization-code"));

    BankIntegrationException exception = assertThrows(
        BankIntegrationException.class,
        () -> bankSimulatorService.getBankResponse(paymentRequest));

    assertEquals("The acquiring bank returned an empty authorized field value.", exception.getMessage());
  }

  private static PostPaymentRequest getPaymentRequest() {
    return new PostPaymentRequest()
        .setCardNumber("2222405343248877")
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency("GBP")
        .setAmount(BigInteger.valueOf(100))
        .setCvv("123");
  }
}
