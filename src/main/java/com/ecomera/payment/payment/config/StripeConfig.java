package com.ecomera.payment.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Configuration
@ConditionalOnProperty(name = "payment.gateway", havingValue = "stripe")
@ConfigurationProperties(prefix = "stripe")
public class StripeConfig {

    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        }
    }
}
