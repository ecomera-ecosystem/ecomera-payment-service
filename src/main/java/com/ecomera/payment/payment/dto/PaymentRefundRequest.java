package com.ecomera.payment.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(name = "PaymentRefundRequest", description = "Payload for refunding a payment (amount optional for full refund)")
public record PaymentRefundRequest(

        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount
) {
}
