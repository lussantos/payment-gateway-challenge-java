package com.checkout.payment.gateway.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.dto.PostPaymentRequest;
import com.checkout.payment.gateway.model.dto.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
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
  @MockBean
  BankSimulatorClient bankSimulatorClient;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = getPaymentResponse();

    paymentsRepository.add(payment);

    MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.card_number_last_four").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiry_month").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiry_year").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency().getCurrencyCode()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount().intValueExact())).andReturn();

    assertEquals(payment, convertResultToObject(result));
  }

  @Test
  void whenPaymentProcessingIsSucceededThenSuccessfulResponseIsReturned() throws Exception {
    PostPaymentRequest payment = getPaymentRequest(Currency.getInstance("USD"));
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "authorization-code");
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
    PostPaymentResponse expectedResponse = new PostPaymentResponse()
        .setId(response.getId())
        .setStatus(PaymentStatus.AUTHORIZED)
        .setCardNumberLastFour("8877")
        .setExpiryMonth(payment.getExpiryMonth())
        .setExpiryYear(payment.getExpiryYear())
        .setCurrency(Currency.getInstance(payment.getCurrency()))
        .setAmount(payment.getAmount());

    assertEquals(expectedResponse, response);

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

  private static PostPaymentResponse getPaymentResponse() {
    return new PostPaymentResponse().setId(ID)
        .setAmount(BigInteger.valueOf(10))
        .setCurrency(Currency.getInstance("USD"))
        .setStatus(PaymentStatus.AUTHORIZED)
        .setExpiryMonth(12)
        .setExpiryYear(2024)
        .setCardNumberLastFour("4321");
  }

  private PostPaymentResponse convertResultToObject(MvcResult result)
      throws JsonProcessingException, UnsupportedEncodingException {
    return objectMapper.readValue(result.getResponse().getContentAsString(),
        PostPaymentResponse.class);
  }

}
