package com.checkout.payment.gateway.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.model.dto.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.PaymentEncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.time.Instant;
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
    PaymentResponse expectedResponse = getPaymentResponse(payment);

    paymentsRepository.save(payment);

    MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/payments/" + payment.getId()))
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
    PaymentRequest payment = getPaymentRequest(Currency.getInstance("USD"));
    BankPaymentResponse bankResponse = new BankPaymentResponse(
        true, "99b8d797-4456-42b4-9b1f-21499d6aaf46");
    when(bankSimulatorClient.processPayment(payment)).thenReturn(bankResponse);

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payment)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.expiry_month").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiry_year").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount().intValueExact()))
        .andReturn();

    PaymentResponse response = convertResultToObject(result);
    Payment storedPayment = paymentsRepository.findById(response.getId()).orElseThrow();
    PaymentResponse expectedResponse = getPaymentResponse(storedPayment);

    assertEquals(expectedResponse, response);

    assertNotEquals(payment.getCardNumber(), storedPayment.getCardNumber());
    assertNotEquals(payment.getCvv(), storedPayment.getCvv());
    assertEquals(
        payment.getCardNumber(), paymentEncryptionService.decrypt(storedPayment.getCardNumber()));
    assertEquals(payment.getCvv(), paymentEncryptionService.decrypt(storedPayment.getCvv()));

    MvcResult retrievalResult = mvc.perform(
            MockMvcRequestBuilders.get("/payments/" + response.getId()))
        .andExpect(status().isOk())
        .andReturn();

    assertEquals(response, convertResultToObject(retrievalResult));
  }

  @Test
  void whenExpiryDateIsNotInTheFutureThenPaymentIsRejected() throws Exception {
    PaymentRequest expiredPayment = getPaymentRequest(Currency.getInstance("GBP"))
        .setExpiryMonth(1)
        .setExpiryYear(2020);

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(expiredPayment)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.error_message").value("Expiry date must be in the future"))
        .andExpect(jsonPath("$.errors[0].field").value("expiry_date"))
        .andExpect(jsonPath("$.errors[0].message").value("Expiry date must be in the future"));

    verify(bankSimulatorClient, never()).processPayment(expiredPayment);
  }

  @Test
  void whenCurrencyIsNotSupportedThenItIsRejectedByTheService() throws Exception {
    PaymentRequest paymentRequestWithUnsupportedCurrency = getPaymentRequest(
        Currency.getInstance("JPY"));
    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(paymentRequestWithUnsupportedCurrency)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.error_message", containsString("Currency 'JPY' is not supported")))
        .andExpect(jsonPath("$.errors[0].field").value("currency"));

    verify(bankSimulatorClient, never()).processPayment(paymentRequestWithUnsupportedCurrency);
  }

  @Test
  void whenMultipleBusinessRulesFailThenEveryErrorIsReturned() throws Exception {
    PaymentRequest invalidPayment = getPaymentRequest(Currency.getInstance("JPY"))
        .setExpiryMonth(1)
        .setExpiryYear(2020);

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidPayment)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.error_message", containsString(
            "Expiry date must be in the future")))
        .andExpect(jsonPath("$.error_message", containsString(
            "Currency 'JPY' is not supported")))
        .andExpect(jsonPath("$.errors[*].field",
            containsInAnyOrder("expiry_date", "currency")));

    verify(bankSimulatorClient, never()).processPayment(invalidPayment);
  }

  private static PaymentRequest getPaymentRequest(Currency currency) {
    return new PaymentRequest()
        .setCardNumber("2222405343248877")
        .setExpiryMonth(12)
        .setExpiryYear(2099)
        .setCurrency(currency.getCurrencyCode())
        .setAmount(BigInteger.valueOf(100))
        .setCvv("123");
  }

  private PaymentResponse getPaymentResponse(Payment payment) {
    String cardNumber = paymentEncryptionService.decrypt(payment.getCardNumber());
    return PaymentResponse.from(payment, cardNumber);
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
        .setCvv(paymentEncryptionService.encrypt("123"))
        .setCreatedAt(Instant.parse("2026-08-09T10:15:30Z"));
  }

  private PaymentResponse convertResultToObject(MvcResult result)
      throws JsonProcessingException, UnsupportedEncodingException {
    return objectMapper.readValue(result.getResponse().getContentAsString(),
        PaymentResponse.class);
  }

}
