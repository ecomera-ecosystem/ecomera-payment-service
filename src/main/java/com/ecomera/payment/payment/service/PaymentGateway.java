package com.ecomera.payment.payment.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    PaymentIntentData createPaymentIntent(BigDecimal amount, String currency, UUID orderId);

    WebhookEvent constructWebhookEvent(String payload, String sigHeader);

    void refundPayment(String paymentIntentId, BigDecimal amount);
}
