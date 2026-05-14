package com.ecomera.payment.payment.dto;

import com.ecomera.payment.payment.enums.PaymentMethod;
import com.ecomera.payment.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "PaymentUpdateRequest", description = "Payload for updating an existing payment")
public record PaymentUpdateRequest(

        @Schema(description = "Updated payment method", example = "CREDIT_CARD", implementation = PaymentMethod.class)
        PaymentMethod paymentMethod,

        @Schema(description = "Updated payment status", example = "REFUNDED", implementation = PaymentStatus.class)
        PaymentStatus status
) {
}
