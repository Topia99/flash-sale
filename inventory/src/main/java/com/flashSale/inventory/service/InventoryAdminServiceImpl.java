package com.flashSale.inventory.service;

import com.flashSale.inventory.domain.Inventory;
import com.flashSale.inventory.dto.InventoryResponse;
import com.flashSale.inventory.exception.ConflictException;
import com.flashSale.inventory.repo.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryAdminServiceImpl implements InventoryAdminService{
    private final InventoryRepository inventoryRepo;


    @Override
    @Transactional
    public InventoryResponse initStock(long ticketId, int available) {
        Inventory inv = inventoryRepo.findById(ticketId).orElse(null);

        if(inv == null) {
            Inventory created = new Inventory();
            created.setTicketId(ticketId);
            created.setAvailable(available);
            created.setReserved(0);
            created.setSold(0);
            created.setVersion(0L);

            Inventory saved = inventoryRepo.save(created);
            return toResponse(saved);
        }

        if (inv.getReserved() != 0 || inv.getSold() != 0){
            throw new ConflictException(
                    "STOCK_ALREADY_IN_USE",
                    "cannot overwrite stock when reserved or sold is non-zero"
            );
        }

        inv.setAvailable(available);
        inv.setVersion(inv.getVersion() + 1);

        Inventory saved = inventoryRepo.save(inv);
        return toResponse(saved);
    }

    private InventoryResponse toResponse(Inventory inv) {
        return new InventoryResponse(
                inv.getTicketId(),
                inv.getAvailable(),
                inv.getReserved(),
                inv.getSold(),
                inv.getVersion()
        );
    }
}
