package com.flashSale.inventory.events;

import lombok.*;

import java.util.List;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class OrderCreatedEvent {
    private EventEnvelope envelope;
    private List<Item> items;

    public record Item(Long ticketId, Integer qty) {}
}
