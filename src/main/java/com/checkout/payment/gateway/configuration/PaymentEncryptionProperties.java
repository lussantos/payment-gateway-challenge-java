package com.checkout.payment.gateway.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payment.encryption")
public record PaymentEncryptionProperties(
    @NotBlank String secret,
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F]{16,}$")
    String salt) {
}
