package com.flashSale.order.exception;

public class CatalogTimeoutException extends RuntimeException {
    public CatalogTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
