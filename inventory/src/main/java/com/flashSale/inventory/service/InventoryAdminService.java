package com.flashSale.inventory.service;

import com.flashSale.inventory.dto.InventoryResponse;

public interface InventoryAdminService {
    InventoryResponse initStock(long ticketId, int available);
}
