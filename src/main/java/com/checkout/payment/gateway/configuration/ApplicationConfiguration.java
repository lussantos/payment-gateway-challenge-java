package com.checkout.payment.gateway.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({PaymentProperties.class, BankSimulatorProperties.class,
    PaymentEncryptionProperties.class})
public class ApplicationConfiguration {

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofMillis(10000))
        .setReadTimeout(Duration.ofMillis(10000))
        .build();
  }

  @Bean
  public TextEncryptor paymentTextEncryptor(PaymentEncryptionProperties properties) {
    return Encryptors.delux(properties.secret(), properties.salt());
  }
}
