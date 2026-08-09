package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
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
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.model.dto.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.math.BigInteger;
import java.time.Instant;
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
  private static final UUID AUTHORIZATION_CODE =
      UUID.fromString("99b8d797-4456-42b4-9b1f-21499d6aaf46");
  private static final Instant PAYMENT_TIME = Instant.parse("2026-08-09T10:15:30Z");
  private static final String CARD_NUMBER = "2222405343248877";
  private static final String CVV = "123";
  private static final String ENCRYPTED_CARD_NUMBER = "encrypted-card-number";
  private static final String ENCRYPTED_CVV = "encrypted-cvv";

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private BankSimulatorService bankSimulatorService;

  @Mock
  private CurrencyValidationService currencyValidationService;

  @Mock
  private PaymentEncryptionService paymentEncryptionService;

  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  @Test
  void returnsPaymentWhenItExists() {
    Payment storedPayment = getPayment(PaymentStatus.AUTHORIZED);
    PaymentResponse expectedResponse = getPaymentResponse(PaymentStatus.AUTHORIZED);
    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.of(storedPayment));
    when(paymentEncryptionService.decrypt(ENCRYPTED_CARD_NUMBER)).thenReturn(CARD_NUMBER);

    PaymentResponse response = paymentGatewayService.getPaymentById(PAYMENT_ID);

    assertEquals(expectedResponse, response);
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
    PaymentRequest paymentRequest = getPaymentRequest();
    Payment expectedPayment = getPayment(expectedStatus);
    PaymentResponse expectedResponse = getPaymentResponse(expectedStatus);
    BankPaymentResponse bankResponse = new BankPaymentResponse(
        authorized, AUTHORIZATION_CODE.toString());

    doNothing().when(currencyValidationService).validate(paymentRequest.getCurrency());
    when(bankSimulatorService.getBankResponse(paymentRequest)).thenReturn(bankResponse);
    when(paymentEncryptionService.encrypt(CARD_NUMBER)).thenReturn(ENCRYPTED_CARD_NUMBER);
    when(paymentEncryptionService.encrypt(CVV)).thenReturn(ENCRYPTED_CVV);
    when(paymentEncryptionService.decrypt(ENCRYPTED_CARD_NUMBER)).thenReturn(CARD_NUMBER);
    doNothing().when(paymentsRepository).add(expectedPayment);

    try (MockedStatic<UUID> uuid = mockStatic(UUID.class, CALLS_REAL_METHODS);
        MockedStatic<Instant> instant = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
      uuid.when(UUID::randomUUID).thenReturn(PAYMENT_ID);
      instant.when(Instant::now).thenReturn(PAYMENT_TIME);

      PaymentResponse response = paymentGatewayService.processPayment(paymentRequest);

      assertEquals(expectedResponse, response);
    }
  }

  @Test
  void rejectsUnsupportedCurrency() {
    PaymentRequest paymentRequest = getPaymentRequest().setCurrency("JPY");
    InvalidPaymentRequestException expectedException =
        new InvalidPaymentRequestException("Currency must be one of: USD, EUR, GBP");
    doThrow(expectedException)
        .when(currencyValidationService).validate(paymentRequest.getCurrency());

    InvalidPaymentRequestException exception = assertThrows(
        InvalidPaymentRequestException.class,
        () -> paymentGatewayService.processPayment(paymentRequest));

    assertSame(expectedException, exception);
    verifyNoInteractions(
        bankSimulatorService, paymentEncryptionService, paymentsRepository);
  }

  @Test
  void rejectsExpiredPaymentBeforeCallingDependencies() {
    PaymentRequest expiredPayment = getPaymentRequest()
        .setExpiryMonth(1)
        .setExpiryYear(2020);

    InvalidPaymentRequestException exception = assertThrows(
        InvalidPaymentRequestException.class,
        () -> paymentGatewayService.processPayment(expiredPayment));

    assertEquals("Expiry date must be in the future", exception.getMessage());
    verifyNoInteractions(
        currencyValidationService, bankSimulatorService, paymentEncryptionService,
        paymentsRepository);
  }

  @Test
  void doesNotPersistPaymentWhenBankIntegrationFails() {
    PaymentRequest paymentRequest = getPaymentRequest();
    BankIntegrationException expectedException =
        new BankIntegrationException("Unable to process payment with the acquiring bank");
    doNothing().when(currencyValidationService).validate(paymentRequest.getCurrency());
    when(bankSimulatorService.getBankResponse(paymentRequest)).thenThrow(expectedException);

    BankIntegrationException exception = assertThrows(
        BankIntegrationException.class,
        () -> paymentGatewayService.processPayment(paymentRequest));

    assertSame(expectedException, exception);
    verifyNoInteractions(paymentEncryptionService, paymentsRepository);
  }

  private static Stream<Arguments> bankDecisions() {
    return Stream.of(
        Arguments.of(true, PaymentStatus.AUTHORIZED),
        Arguments.of(false, PaymentStatus.DECLINED));
  }

  private static PaymentRequest getPaymentRequest() {
    return new PaymentRequest()
        .setCardNumber(CARD_NUMBER)
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency("GBP")
        .setAmount(BigInteger.valueOf(100))
        .setCvv(CVV);
  }

  private static Payment getPayment(PaymentStatus status) {
    return new Payment()
        .setId(PAYMENT_ID)
        .setStatus(status)
        .setCardNumber(ENCRYPTED_CARD_NUMBER)
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency("GBP")
        .setAmount(BigInteger.valueOf(100))
        .setCvv(ENCRYPTED_CVV)
        .setAuthorizationCode(status == PaymentStatus.AUTHORIZED ? AUTHORIZATION_CODE : null)
        .setCreated(PAYMENT_TIME)
        .setUpdated(PAYMENT_TIME);
  }

  private static PaymentResponse getPaymentResponse(PaymentStatus status) {
    return new PaymentResponse()
        .setId(PAYMENT_ID)
        .setStatus(status)
        .setCardNumberLastFour("8877")
        .setExpiryMonth(4)
        .setExpiryYear(2099)
        .setCurrency(Currency.getInstance("GBP"))
        .setAmount(BigInteger.valueOf(100));
  }
}
