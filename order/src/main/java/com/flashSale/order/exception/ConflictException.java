package com.flashSale.order.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message)
    {
        super(message);
    }
}
