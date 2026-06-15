package com.ecomera.payment.payment.dto;

import com.ecomera.payment.payment.enums.PaymentMethod;
import com.ecomera.payment.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Schema(name = "PaymentDto", description = "Represents a payment with its current status")
public record PaymentDto(
        UUID id,
        UUID orderId,
        UUID userId,
        String email,
        String stripePaymentIntentId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
