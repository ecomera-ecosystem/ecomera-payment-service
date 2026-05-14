package com.ecomera.payment.payment.dto;

import com.ecomera.payment.payment.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(name = "PaymentCreateRequest", description = "Payload for creating a payment intent")
public record PaymentCreateRequest(

        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotNull(message = "Payment method is required")
        @Schema(description = "Payment method (enum name in uppercase)", example = "CREDIT_CARD", implementation = PaymentMethod.class)
        PaymentMethod paymentMethod
) {
}
