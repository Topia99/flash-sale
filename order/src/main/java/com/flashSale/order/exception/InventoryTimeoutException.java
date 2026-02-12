package com.flashSale.order.exception;

public class InventoryTimeoutException extends RuntimeException {
    public InventoryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
