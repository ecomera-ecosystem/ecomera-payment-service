package com.ecomera.payment.shared.common.exception;

import org.springframework.http.HttpStatus;

public class AlreadyExistException extends ApiException {

    public AlreadyExistException(Class<?> resource, String fieldName, Object fieldValue) {
        super(resource.getSimpleName() + " already exists with " + fieldName + ": " + fieldValue,
                HttpStatus.CONFLICT);
    }

    public AlreadyExistException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
