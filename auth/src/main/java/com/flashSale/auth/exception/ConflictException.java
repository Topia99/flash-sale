package com.flashSale.auth.exception;

public class ConflictException extends RuntimeException {
    public final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}