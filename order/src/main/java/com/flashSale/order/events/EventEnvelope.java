package com.flashSale.order.events;

import com.flashSale.order.domain.Order;
import jdk.jfr.EventType;
import lombok.*;

import java.time.Instant;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor @Builder
public class EventEnvelope {
    private String eventId;

    private OrderEventType eventType;

    private Instant occurredAt;

    private String traceId;

    private Long orderId;
    private Long userId;

    private String idempotencyKey;
}
