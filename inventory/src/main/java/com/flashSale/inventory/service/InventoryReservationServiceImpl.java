package com.flashSale.inventory.service;

import com.flashSale.inventory.domain.InventoryReservation;
import com.flashSale.inventory.domain.ReservationStatus;
import com.flashSale.inventory.dto.ReservationResponse;
import com.flashSale.inventory.exception.ConflictException;
import com.flashSale.inventory.exception.NotFoundException;
import com.flashSale.inventory.repo.InventoryRepository;
import com.flashSale.inventory.repo.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReservationServiceImpl implements InventoryReservationService{
    private final InventoryRepository inventoryRepo;
    private final ReservationRepository reservationRepo;

    /**
     * Reserve is idempotent by reservationId
     *
     * Tx order:
     * 1) Insert reservation (INIT) - "occupy the idempotency slot"
     * 2) Atomic update inventory (available>=qty)
     * 3) Update reservation status -> RESERVED or FAILED
     */
    @Override
    @Transactional
    public ReservationResponse reserve(String reservationId, long ticketId, int qty) {

        // 0) Idempotency fast path
        InventoryReservation existing = reservationRepo.findById(reservationId).orElse(null);
        if (existing != null){
            return toResponse(existing);
        }

        // 1) Create INIT reservation (handle concurrent duplicate reservationId)
        InventoryReservation created = new InventoryReservation();
        created.setReservationId(reservationId);
        created.setTicketId(ticketId);
        created.setQty(qty);
        created.setStatus(ReservationStatus.INIT);

        try {
            reservationRepo.saveAndFlush(created);
        } catch (DataIntegrityViolationException dup) {
            // Another request inserted same reservationId concurrently
            InventoryReservation raced = reservationRepo.findById(reservationId)
                    .orElseThrow(() -> dup);
            return toResponse(raced);
        }

        // 2) Ticket existence check (keep behavior deterministic: 404 if not initialized)
        if(!inventoryRepo.existsById(ticketId)) {
            created.setStatus(ReservationStatus.FAILED);
            reservationRepo.save(created);

            log.warn("reserve failed: ticket not found. reservationId={}, ticketId={}, qty={}",
                    reservationId, ticketId, qty);

            throw new NotFoundException("TICKET_NOT_FOUND", "ticket stock not initialized");
        }

        // 3) Atomic reserve (A1)
        int rows = inventoryRepo.reserveAtomic(ticketId, qty);

        if(rows == 1){
            created.setStatus(ReservationStatus.RESERVED);
            reservationRepo.save(created);

            log.info("reserve success: reservationId={}, ticketId={}, qty={}",
                    reservationId, ticketId, qty);

            return toResponse(created);
        }

        // 4) Insufficient stock -> FAILED (terminal)
        created.setStatus(ReservationStatus.FAILED);
        reservationRepo.save(created);

        log.info("reserve insufficient: reservationId={}, ticketId={}, qty={}",
                reservationId, ticketId, qty);

        throw new ConflictException("INSUFFICIENT_STOCK", "insufficient stock");
    }

    private ReservationResponse toResponse(InventoryReservation r){
        return new ReservationResponse(
                r.getReservationId(),
                r.getTicketId(),
                r.getQty(),
                r.getStatus()
        );
    }
}
