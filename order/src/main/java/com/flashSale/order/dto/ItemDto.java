package com.flashSale.order.dto;

import com.flashSale.order.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ItemDto {
    private Long ticketId;
    private Integer qty;
    private BigDecimal unitPrice;

    public ItemDto(OrderItem oi) {
        this(oi.getTicketId(), oi.getQty(), oi.getUnitPrice());
    }
}
