package com.checkout.payment.gateway.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.dto.PostPaymentRequest;
import com.checkout.payment.gateway.model.dto.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.PaymentGatewayService;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
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
  @SpyBean
  PaymentGatewayService paymentGatewayService;

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
  void whenPaymentProcessingIsSucceededThenSucessfullResponseIsReturned() throws Exception {
    PostPaymentRequest payment = getPaymentRequest(Currency.getInstance("USD"));

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payment)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(getPaymentResponse().getId().toString()))
        .andExpect(jsonPath("$.status").value(getPaymentResponse().getStatus().getName()))
        .andExpect(
            jsonPath("$.card_number_last_four").value(getPaymentResponse().getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiry_month").value(getPaymentResponse().getExpiryMonth()))
        .andExpect(jsonPath("$.expiry_year").value(getPaymentResponse().getExpiryYear()))
        .andExpect(
            jsonPath("$.currency").value(getPaymentResponse().getCurrency().getCurrencyCode()))
        .andExpect(jsonPath("$.amount").value(getPaymentResponse().getAmount().intValueExact()))
        .andReturn();

    assertEquals(getPaymentResponse(), convertResultToObject(result));
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

    verify(paymentGatewayService).processPayment(paymentRequestWithUnsupportedCurrency);
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
        .setCardNumberLastFour(4321);
  }

  private PostPaymentResponse convertResultToObject(MvcResult result)
      throws JsonProcessingException, UnsupportedEncodingException {
    return objectMapper.readValue(result.getResponse().getContentAsString(),
        PostPaymentResponse.class);
  }

}
