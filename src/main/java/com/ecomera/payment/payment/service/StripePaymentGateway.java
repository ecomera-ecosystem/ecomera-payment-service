package com.ecomera.payment.payment.service;

import com.ecomera.payment.shared.common.exception.BusinessException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "payment.gateway", havingValue = "stripe")
public class StripePaymentGateway implements PaymentGateway {

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Override
    public PaymentIntentData createPaymentIntent(BigDecimal amount, String currency, UUID orderId) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .putMetadata("order_id", orderId.toString())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            log.info("Stripe PaymentIntent created: {} for order: {}", intent.getId(), orderId);
            return new PaymentIntentData(intent.getId());
        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent for order {}: {}", orderId, e.getMessage());
            throw new BusinessException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public WebhookEvent constructWebhookEvent(String payload, String sigHeader) {
        try {
            com.stripe.model.Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            String eventType = event.getType();
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new BusinessException("Failed to deserialize PaymentIntent from webhook"));
            return new WebhookEvent(eventType, intent.getId());
        } catch (Exception e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            throw new BusinessException("Invalid webhook signature", e);
        }
    }

    @Override
    public void refundPayment(String paymentIntentId, BigDecimal amount) {
        try {
            RefundCreateParams params;
            if (amount != null) {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                params = RefundCreateParams.builder()
                        .setPaymentIntent(paymentIntentId)
                        .setAmount(amountInCents)
                        .build();
            } else {
                params = RefundCreateParams.builder()
                        .setPaymentIntent(paymentIntentId)
                        .build();
            }

            Refund refund = Refund.create(params);
            log.info("Stripe refund created: {} for PaymentIntent: {}", refund.getId(), paymentIntentId);
        } catch (StripeException e) {
            log.error("Failed to refund PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            throw new BusinessException("Refund failed: " + e.getMessage(), e);
        }
    }
}
