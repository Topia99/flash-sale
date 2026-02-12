package com.flashSale.order.dto;

public record ReserveRequest(
        String reservationId,
        Long ticketId,
        Integer qty
) {
}
