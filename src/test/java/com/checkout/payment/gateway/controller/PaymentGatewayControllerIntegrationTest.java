package com.checkout.payment.gateway.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
            .header("Idempotency-Key", UUID.randomUUID().toString())
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
  void sameIdempotencyKeyAndRequestReplaysTheOriginalPayment() throws Exception {
    PaymentRequest payment = getPaymentRequest(Currency.getInstance("USD"));
    String key = UUID.randomUUID().toString();
    when(bankSimulatorClient.processPayment(payment))
        .thenReturn(new BankPaymentResponse(false, null));

    PaymentResponse first = postPayment(key, payment, 201);
    PaymentResponse replay = postPayment(key, payment, 201);

    assertEquals(first, replay);
    verify(bankSimulatorClient, times(1)).processPayment(payment);
  }

  @Test
  void sameIdempotencyKeyWithDifferentRequestIsRejected() throws Exception {
    PaymentRequest firstRequest = getPaymentRequest(Currency.getInstance("USD"));
    PaymentRequest differentRequest = getPaymentRequest(Currency.getInstance("USD"))
        .setAmount(BigInteger.valueOf(101));
    String key = UUID.randomUUID().toString();
    when(bankSimulatorClient.processPayment(firstRequest))
        .thenReturn(new BankPaymentResponse(false, null));

    postPayment(key, firstRequest, 201);

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(differentRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(
            "Idempotency-Key has already been used with a different request"));
    verify(bankSimulatorClient, times(1)).processPayment(firstRequest);
  }

  @Test
  void concurrentRequestsWithSameKeyExecuteBankCallOnce() throws Exception {
    PaymentRequest payment = getPaymentRequest(Currency.getInstance("GBP"));
    String key = UUID.randomUUID().toString();
    CountDownLatch start = new CountDownLatch(1);
    when(bankSimulatorClient.processPayment(payment)).thenAnswer(invocation -> {
      Thread.sleep(150);
      return new BankPaymentResponse(false, null);
    });

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Future<PaymentResponse>> futures = List.of(
          executor.submit(() -> {
            start.await();
            return postPayment(key, payment, 201);
          }),
          executor.submit(() -> {
            start.await();
            return postPayment(key, payment, 201);
          }));
      start.countDown();

      assertEquals(futures.get(0).get(), futures.get(1).get());
    } finally {
      executor.shutdownNow();
    }
    verify(bankSimulatorClient, times(1)).processPayment(payment);
  }

  @Test
  void concurrentRequestsWithDifferentKeysDoNotBlockEachOther() throws Exception {
    PaymentRequest payment = getPaymentRequest(Currency.getInstance("EUR"));
    CountDownLatch bothBankCallsStarted = new CountDownLatch(2);
    when(bankSimulatorClient.processPayment(payment)).thenAnswer(invocation -> {
      bothBankCallsStarted.countDown();
      assertTrue(bothBankCallsStarted.await(1, TimeUnit.SECONDS));
      return new BankPaymentResponse(false, null);
    });

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<PaymentResponse> first = executor.submit(
          () -> postPayment(UUID.randomUUID().toString(), payment, 201));
      Future<PaymentResponse> second = executor.submit(
          () -> postPayment(UUID.randomUUID().toString(), payment, 201));

      assertNotEquals(first.get().getId(), second.get().getId());
    } finally {
      executor.shutdownNow();
    }
    verify(bankSimulatorClient, times(2)).processPayment(payment);
  }

  @Test
  void whenExpiryDateIsNotInTheFutureThenPaymentIsRejected() throws Exception {
    PaymentRequest expiredPayment = getPaymentRequest(Currency.getInstance("GBP"))
        .setExpiryMonth(1)
        .setExpiryYear(2020);

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", UUID.randomUUID().toString())
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
            .header("Idempotency-Key", UUID.randomUUID().toString())
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
            .header("Idempotency-Key", UUID.randomUUID().toString())
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

  private PaymentResponse postPayment(String idempotencyKey, PaymentRequest payment,
      int expectedStatus) throws Exception {
    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payment)))
        .andExpect(status().is(expectedStatus))
        .andReturn();
    return convertResultToObject(result);
  }

}
