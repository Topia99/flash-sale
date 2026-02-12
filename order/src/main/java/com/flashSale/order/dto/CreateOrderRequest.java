package com.flashSale.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty @Valid List<Item> items,
        String currency
        ) {
    public record Item(
            @NotNull @Positive Long ticketId,
            @NotNull @Positive Integer qty
    ) {}
}
