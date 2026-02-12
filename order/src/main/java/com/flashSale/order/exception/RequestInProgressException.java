package com.flashSale.order.exception;

public class RequestInProgressException extends RuntimeException {
    public RequestInProgressException() {
        super("REQUEST_IN_PROGRESS");
    }
}
