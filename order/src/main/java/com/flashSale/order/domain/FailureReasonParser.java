package com.flashSale.order.domain;

public class FailureReasonParser {
    private FailureReasonParser() {}

    public static FailureReason parseOrDefault(String code) {
        if (code == null || code.isBlank()) return FailureReason.INVENTORY_ERROR;

        try {
            return FailureReason.valueOf(code);
        } catch (IllegalArgumentException ex) {
            // Unknown code -> don't crash consumer; default to INVENTORY_ERROR
            return FailureReason.INVENTORY_ERROR;
        }
    }
}
