package com.ecomera.payment.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "payment.gateway", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PaymentIntentData createPaymentIntent(BigDecimal amount, String currency, UUID orderId) {
        String mockId = "pi_mock_" + UUID.randomUUID();
        log.info("[MOCK] PaymentIntent created: {} for order: {} (amount={} {})",
                mockId, orderId, amount, currency);
        return new PaymentIntentData(mockId);
    }

    @Override
    public WebhookEvent constructWebhookEvent(String payload, String sigHeader) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.has("type") ? root.get("type").asText() : "payment_intent.succeeded";
            String paymentIntentId = root.has("payment_intent_id")
                    ? root.get("payment_intent_id").asText()
                    : "pi_mock_unknown";
            log.info("[MOCK] Webhook event constructed: type={}, paymentIntentId={}", type, paymentIntentId);
            return new WebhookEvent(type, paymentIntentId);

        } catch (Exception e) {
            log.warn("[MOCK] Failed to parse webhook payload: {}. Defaulting to SUCCEEDED.", e.getMessage());
            return new WebhookEvent("payment_intent.succeeded", "pi_mock_default");
        }
    }

    @Override
    public void refundPayment(String paymentIntentId, BigDecimal amount) {
        if (amount != null) {
            log.info("[MOCK] Refund processed: {} for PaymentIntent: {} (amount={})",
                    "ref_mock_" + UUID.randomUUID(), paymentIntentId, amount);
        } else {
            log.info("[MOCK] Full refund processed: {} for PaymentIntent: {}",
                    "ref_mock_" + UUID.randomUUID(), paymentIntentId);
        }
    }
}
