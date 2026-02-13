package com.flashSale.order.events;

import lombok.*;

import java.util.List;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor @Builder
public class OrderCreatedEvent {

    private EventEnvelope envelope;

    private List<Item> items;

    public record Item(
            Long ticketId,
            Integer qty
    ){}
}
