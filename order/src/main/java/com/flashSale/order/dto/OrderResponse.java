package com.flashSale.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long userId,
        String status,
        String failureReason,
        String currency,
        BigDecimal totalAmount,
        String idempotencyKey,
        List<Item> items,
        Instant createdAt
) {
    public record Item(
            Long ticketId,
            Integer qty,
            BigDecimal unitPrice
    ) {}
}
