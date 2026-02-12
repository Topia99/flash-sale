package com.flashSale.order.exception;

public class InventoryErrorException extends RuntimeException {
    public InventoryErrorException(String message) {
        super(message);
    }
}
