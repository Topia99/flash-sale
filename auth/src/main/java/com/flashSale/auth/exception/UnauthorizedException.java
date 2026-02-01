package com.flashSale.auth.exception;

public class UnauthorizedException extends RuntimeException {
    public final String code;

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }
}
