package com.checkout.payment.gateway.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(

    @NotEmpty
    @Size(max = 3)
    Set<@Pattern(regexp = "^[A-Z]{3}$") String> supportedCurrencies) {
}
