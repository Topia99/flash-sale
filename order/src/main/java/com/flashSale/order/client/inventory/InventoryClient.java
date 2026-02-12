package com.flashSale.order.client.inventory;

import com.flashSale.order.dto.ReservationResponse;

public interface InventoryClient {
    ReservationResponse reserve(String reservationId, Long ticketId, int qty);
    ReservationResponse release(String reservationId);
    ReservationResponse commit(String reservationId);
}
