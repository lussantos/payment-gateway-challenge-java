package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.model.dto.PaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/payments/{id}")
  public ResponseEntity<PaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }

  @PostMapping("/payments")
  public ResponseEntity<PaymentResponse> processPayment(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody PaymentRequest paymentRequest) {
    return new ResponseEntity<>(
        paymentGatewayService.processPayment(idempotencyKey, paymentRequest), HttpStatus.CREATED);
  }

}
