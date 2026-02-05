package com.flashSale.inventory.service;

import com.flashSale.inventory.dto.ReservationResponse;

public interface InventoryReservationService {
    ReservationResponse reserve(String reservationId, long ticketId, int qty);
    ReservationResponse release(String reservationId);
}
