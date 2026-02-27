package com.flashSale.inventory.saga;

import com.flashSale.inventory.events.OrderEventType;

public record InventorySagaResult(
        OrderEventType eventType,
        InventoryFailReason reason
) {

    public static InventorySagaResult committed() {
        return new InventorySagaResult(OrderEventType.STOCK_COMMITTED, null);
    }

    public static InventorySagaResult failed(InventoryFailReason reason) {
        return new InventorySagaResult(OrderEventType.STOCK_FAILED, reason);
    }
}
