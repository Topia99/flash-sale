package com.flashSale.inventory.events;

import com.flashSale.inventory.saga.InventoryFailReason;

public final class InventoryFailureReasonTranslator {
    private InventoryFailureReasonTranslator() {}

    public static String toReasonCode(InventoryFailReason r) {
        if(r==null) return "INVENTORY_ERROR";

        return switch (r) {
            case OUT_OF_STOCK -> "INSUFFICIENT_STOCK";
            case INVALID_REQUEST -> "INVALID_REQUEST";
            case INTERNAL_ERROR -> "INVENTORY_ERROR";
        };
    }
}
