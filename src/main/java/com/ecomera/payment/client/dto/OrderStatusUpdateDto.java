package com.ecomera.payment.client.dto;

import lombok.Builder;

@Builder
public record OrderStatusUpdateDto(
        String status
) {
}
