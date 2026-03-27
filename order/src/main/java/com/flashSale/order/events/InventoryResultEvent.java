package com.flashSale.order.events;

import lombok.*;

import java.util.List;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor @Builder
public class InventoryResultEvent {

    private EventEnvelope envelope;

    private List<OrderCreatedEvent.Item> items;

    private String reasonCode;
}
