package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.dto.BankPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import com.checkout.payment.gateway.model.dto.PostPaymentRequest;
import com.checkout.payment.gateway.model.dto.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.math.BigInteger;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  private static final UUID PAYMENT_ID =
      UUID.fromString("83173e49-4c3f-4952-9df4-1b2d675a5ba5");

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private BankSimulatorService bankSimulatorService;

  @Mock
  private CurrencyValidationService currencyValidationService;

  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  @Test
  void returnsPaymentWhenItExists() {
    PostPaymentResponse expectedPayment = getPaymentResponse(PAYMENT_ID);
    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.of(expectedPayment));

    PostPaymentResponse payment = paymentGatewayService.getPaymentById(PAYMENT_ID);

    assertSame(expectedPayment, payment);
  }

  @Test
  void rejectsPaymentIdWhenItDoesNotExist() {
    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.empty());

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> paymentGatewayService.getPaymentById(PAYMENT_ID));

    assertEquals("Invalid ID", exception.getMessage());
  }

  @ParameterizedTest(name = "bank authorized={0} maps to {1}")
  @MethodSource("bankDecisions")
  void processesBankDecision(boolean authorized, PaymentStatus expectedStatus) {
    PostPaymentRequest paymentRequest = getPaymentRequest();
    PostPaymentResponse expectedResponse = getPaymentResponse(PAYMENT_ID).setStatus(expectedStatus);
    BankPaymentResponse bankResponse = new BankPaymentResponse(
        authorized, authorized ? "authorization-code" : null);

    doNothing().when(currencyValidationService).validate(paymentRequest.getCurrency());
    when(bankSimulatorService.getBankResponse(paymentRequest)).thenReturn(bankResponse);
    doNothing().when(paymentsRepository).add(expectedResponse);

    try (MockedStatic<UUID> uuid = mockStatic(UUID.class)) {
      uuid.when(UUID::randomUUID).thenReturn(PAYMENT_ID);

      PostPaymentResponse response = paymentGatewayService.processPayment(paymentRequest);

      assertEquals(expectedResponse, response);
    }
  }

  @Test
  void rejectsUnsupportedCurrency() {
    PostPaymentRequest paymentRequest = getPaymentRequest().setCurrency("JPY");
    InvalidPaymentRequestException expectedException =
        new InvalidPaymentRequestException("Currency must be one of: USD, EUR, GBP");
    doThrow(expectedException)
        .when(currencyValidationService).validate(paymentRequest.getCurrency());

    InvalidPaymentRequestException exception = assertThrows(
        InvalidPaymentRequestException.class,
        () -> paymentGatewayService.processPayment(paymentRequest));

    assertSame(expectedException, exception);
    verifyNoInteractions(bankSimulatorService, paymentsRepository);
  }

  @Test
  void rejectsExpiredPaymentBeforeCallingDependencies() {
    PostPaymentRequest expiredPayment = getPaymentRequest()
        .setExpiryMonth(1)
        .setExpiryYear(2020);

    InvalidPaymentRequestException exception = assertThrows(
        InvalidPaymentRequestException.class,
        () -> paymentGatewayService.processPayment(expiredPayment));

    assertEquals("Expiry date must be in the future", exception.getMessage());
    verifyNoInteractions(
        currencyValidationService, bankSimulatorService, paymentsRepository);
  }

  @Test
  void doesNotPersistPaymentWhenBankIntegrationFails() {
    PostPaymentRequest paymentRequest = getPaymentRequest();
    BankIntegrationException expectedException =
        new BankIntegrationException("Unable to process payment with the acquiring bank");
    doNothing().when(currencyValidationService).validate(paymentRequest.getCurrency());
    when(bankSimulatorService.getBankResponse(paymentRequest)).thenThrow(expectedException);

    BankIntegrationException exception = assertThrows(
        BankIntegrationException.class,
        () -> paymentGatewayService.processPayment(paymentRequest));

    assertSame(expectedException, exception);
    verifyNoInteractions(paymentsRepository);
  }

  private static Stream<Arguments> bankDecisions() {
    return Stream.of(
        Arguments.of(true, PaymentStatus.AUTHORIZED),
        Arguments.of(false, PaymentStatus.DECLINED));
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

  private static PostPaymentResponse getPaymentResponse(UUID id) {
    return new PostPaymentResponse()
        .setId(id)
        .setStatus(PaymentStatus.AUTHORIZED)
        .setCardNumberLastFour("8877")
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency(Currency.getInstance("GBP"))
        .setAmount(BigInteger.valueOf(100));
  }
}
