package com.flashSale.inventory.events;

import java.util.List;

public record InventoryResultEvent(
        EventEnvelope envelope,
        List<OrderCreatedEvent.Item> items,
        String reasonCode
) {
}
