package com.flashSale.order.dto;

public record ReservationResponse(
        String reservationId,
        String status,
        Long ticketId,
        Integer qty
) {
}
