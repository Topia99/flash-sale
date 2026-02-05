package com.flashSale.inventory.dto;

public record InventoryResponse(
        Long tickerId,
        int available,
        int reserved,
        int sold,
        Long version
) {
}
