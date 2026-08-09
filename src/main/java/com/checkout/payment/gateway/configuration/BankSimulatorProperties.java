package com.checkout.payment.gateway.configuration;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bank-simulator")
public record BankSimulatorProperties(
    @NotNull URI paymentsUrl) {
}
