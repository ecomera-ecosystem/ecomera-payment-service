package com.ecomera.payment.payment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Schema(name = "PaymentStatus", description = "Represents the lifecycle status of a payment")
public enum PaymentStatus {

    PENDING("Pending"),
    SUCCEEDED("Succeeded"),
    FAILED("Failed"),
    REFUNDED("Refunded"),
    PARTIALLY_REFUNDED("Partially Refunded");

    @JsonValue
    private final String statusName;

    public static Optional<PaymentStatus> fromString(String value) {
        return Arrays.stream(values())
                .filter(ps -> ps.name().equalsIgnoreCase(value) || ps.getStatusName().equalsIgnoreCase(value))
                .findFirst();
    }

    @JsonCreator
    public static PaymentStatus forValue(String value) {
        return fromString(value)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid status: " + value + ". Valid values are: " +
                                Arrays.stream(values()).map(PaymentStatus::getStatusName).collect(Collectors.joining(", "))
                ));
    }
}
