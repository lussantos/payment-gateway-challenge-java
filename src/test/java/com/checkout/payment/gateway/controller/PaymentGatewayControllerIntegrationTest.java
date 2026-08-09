package com.checkout.payment.gateway.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.dto.PostPaymentRequest;
import com.checkout.payment.gateway.model.dto.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.PaymentEncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerIntegrationTest {

  private static final UUID ID = UUID.fromString("bede9e5d-d54f-4e99-b73b-a30941cc9048");

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private PaymentEncryptionService paymentEncryptionService;
  @MockBean
  BankSimulatorClient bankSimulatorClient;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    Payment payment = getStoredPayment();
    PostPaymentResponse expectedResponse = getPaymentResponse(payment);

    paymentsRepository.add(payment);

    MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(expectedResponse.getStatus().getName()))
        .andExpect(jsonPath("$.card_number_last_four")
            .value(expectedResponse.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiry_month").value(expectedResponse.getExpiryMonth()))
        .andExpect(jsonPath("$.expiry_year").value(expectedResponse.getExpiryYear()))
        .andExpect(jsonPath("$.currency")
            .value(expectedResponse.getCurrency().getCurrencyCode()))
        .andExpect(jsonPath("$.amount").value(expectedResponse.getAmount().intValueExact()))
        .andReturn();

    assertEquals(expectedResponse, convertResultToObject(result));
  }

  @Test
  void whenPaymentProcessingIsSucceededThenSuccessfulResponseIsReturned() throws Exception {
    PostPaymentRequest payment = getPaymentRequest(Currency.getInstance("USD"));
    BankPaymentResponse bankResponse = new BankPaymentResponse(
        true, "99b8d797-4456-42b4-9b1f-21499d6aaf46");
    when(bankSimulatorClient.processPayment(payment)).thenReturn(bankResponse);

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payment)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.expiry_month").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiry_year").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount().intValueExact()))
        .andReturn();

    PostPaymentResponse response = convertResultToObject(result);
    Payment storedPayment = paymentsRepository.get(response.getId()).orElseThrow();
    PostPaymentResponse expectedResponse = getPaymentResponse(storedPayment);

    assertEquals(expectedResponse, response);

    assertNotEquals(payment.getCardNumber(), storedPayment.getCardNumber());
    assertNotEquals(payment.getCvv(), storedPayment.getCvv());
    assertEquals(
        payment.getCardNumber(), paymentEncryptionService.decrypt(storedPayment.getCardNumber()));
    assertEquals(payment.getCvv(), paymentEncryptionService.decrypt(storedPayment.getCvv()));

    MvcResult retrievalResult = mvc.perform(
            MockMvcRequestBuilders.get("/payment/" + response.getId()))
        .andExpect(status().isOk())
        .andReturn();

    assertEquals(response, convertResultToObject(retrievalResult));
  }

  @Test
  void whenExpiryDateIsNotInTheFutureThenPaymentIsRejected() throws Exception {
    PostPaymentRequest expiredPayment = getPaymentRequest(Currency.getInstance("GBP"))
        .setExpiryMonth(1)
        .setExpiryYear(2020);

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(expiredPayment)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.error_message").value("Expiry date must be in the future"));

    verify(bankSimulatorClient, never()).processPayment(expiredPayment);
  }

  @Test
  void whenCurrencyIsNotSupportedThenItIsRejectedByTheService() throws Exception {
    PostPaymentRequest paymentRequestWithUnsupportedCurrency = getPaymentRequest(
        Currency.getInstance("JPY"));
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(paymentRequestWithUnsupportedCurrency)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.error_message", containsString("Currency must be one of:")));

    verify(bankSimulatorClient, never()).processPayment(paymentRequestWithUnsupportedCurrency);
  }

  private static PostPaymentRequest getPaymentRequest(Currency currency) {
    return new PostPaymentRequest()
        .setCardNumber("2222405343248877")
        .setExpiryMonth(12)
        .setExpiryYear(2099)
        .setCurrency(currency.getCurrencyCode())
        .setAmount(BigInteger.valueOf(100))
        .setCvv("123");
  }

  private PostPaymentResponse getPaymentResponse(Payment payment) {
    String cardNumber = paymentEncryptionService.decrypt(payment.getCardNumber());
    return PostPaymentResponse.from(payment, cardNumber);
  }

  private Payment getStoredPayment() {
    return new Payment()
        .setId(ID)
        .setStatus(PaymentStatus.AUTHORIZED)
        .setCardNumber(paymentEncryptionService.encrypt("2222405343244321"))
        .setExpiryMonth(12)
        .setExpiryYear(2024)
        .setCurrency("USD")
        .setAmount(BigInteger.valueOf(10))
        .setCvv(paymentEncryptionService.encrypt("123"));
  }

  private PostPaymentResponse convertResultToObject(MvcResult result)
      throws JsonProcessingException, UnsupportedEncodingException {
    return objectMapper.readValue(result.getResponse().getContentAsString(),
        PostPaymentResponse.class);
  }

}
