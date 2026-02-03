package com.flashSale.catalog.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
