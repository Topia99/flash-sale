package com.flashSale.inventory.dto;

import com.flashSale.inventory.domain.ReservationStatus;

public record ReservationResponse(
        String reservationId,
        long ticketId,
        int qty,
        ReservationStatus status
) {
}
