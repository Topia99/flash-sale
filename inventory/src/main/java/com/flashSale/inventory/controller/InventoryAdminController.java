package com.flashSale.inventory.controller;

import com.flashSale.inventory.dto.InitStockRequest;
import com.flashSale.inventory.dto.InventoryResponse;
import com.flashSale.inventory.service.InventoryAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory/admin")
public class InventoryAdminController {
    private final InventoryAdminService inventoryAdminService;

    @PutMapping("/stocks/{ticketId}")
    public InventoryResponse InitStock(
            @PathVariable("ticketId") long ticketId,
            @Valid @RequestBody InitStockRequest request){
        return inventoryAdminService.initStock(ticketId, request.getAvailable());
    }
}
