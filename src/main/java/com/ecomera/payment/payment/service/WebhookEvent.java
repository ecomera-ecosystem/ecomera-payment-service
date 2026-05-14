package com.ecomera.payment.payment.service;

public record WebhookEvent(String type, String paymentIntentId) {
}
