package com.checkout.payment.gateway.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.dto.PostPaymentErrorResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @SpyBean
  PaymentGatewayService paymentGatewayService;

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Page not found"));
  }

  @ParameterizedTest(name = "[{index}] rejects invalid payment request")
  @MethodSource("invalidPaymentRequests")
  void whenPaymentRequestIsInvalidThenItIsRejectedWithoutProcessing(String request,
      List<String> requiredErrorMessages) throws Exception {

    MvcResult result = mvc.perform(
            MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
                .content(request)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName())).andReturn();

    PostPaymentErrorResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), PostPaymentErrorResponse.class);
    requiredErrorMessages.forEach(
        requiredMessage -> assertTrue(response.getErrorMessage().contains(requiredMessage),
            () -> "Expected error message to contain: " + requiredMessage));

    verify(paymentGatewayService, never()).processPayment(any());
  }

  private static Stream<Arguments> invalidPaymentRequests() {
    return Stream.of(Arguments.of("""
        {
          "card_number": "2222405343248877",
          "expiry_month": 12,
          "expiry_year": 2099,
          "currency": "GBP",
          "amount": 0.5,
          "cvv": "123"
        }
        """, List.of("Payment request body is malformed")), Arguments.of("{}",
        List.of("Card number is required", "Expiry month is required", "Expiry year is required",
            "Currency is required", "Amount is required", "CVV is required")), Arguments.of("""
        {
          "card_number": "2222405343248877",
          "expiry_month": 1,
          "expiry_year": 2020,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """, List.of("Expiry date must be in the future")), Arguments.of("""
        {
          "card_number": "1234",
          "expiry_month": 13,
          "expiry_year": 2020,
          "currency": "JPY",
          "amount": 100,
          "cvv": "12"
        }
        """, List.of("Card number", "Expiry month", "CVV")));
  }

}
