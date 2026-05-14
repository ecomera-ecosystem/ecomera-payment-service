package com.ecomera.payment.client.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderDto(
        UUID id,
        BigDecimal totalPrice
) {
}
