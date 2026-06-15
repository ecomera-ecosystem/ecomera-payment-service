package com.ecomera.payment.payment.dto;

import com.ecomera.payment.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "PaymentUpdateRequest", description = "Payload for updating payment status")
public record PaymentUpdateRequest(

        @Schema(description = "Updated payment status", example = "SUCCEEDED", implementation = PaymentStatus.class)
        PaymentStatus status
) {
}
