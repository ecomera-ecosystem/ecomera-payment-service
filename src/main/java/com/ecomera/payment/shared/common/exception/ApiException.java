package com.ecomera.payment.shared.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ApiException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public ApiException(Class<?> resource, String fieldName, Object fieldValue, HttpStatus status) {
        super(String.format("%s error with %s: '%s'", resource.getSimpleName(), fieldName, fieldValue));
        this.status = status;
    }
}
