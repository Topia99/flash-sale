package com.flashSale.inventory.events;

import com.flashSale.inventory.saga.InventoryFailReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryResultPublisher {

    private static final String TOPIC = "inventory.result.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCommitted(EventEnvelope fromOrderEnvelope,
                                 java.util.List<OrderCreatedEvent.Item> items) {

        EventEnvelope env = EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OrderEventType.STOCK_COMMITTED)
                .occurredAt(Instant.now())
                .traceId(fromOrderEnvelope.getTraceId())
                .userId(fromOrderEnvelope.getUserId())
                .orderId(fromOrderEnvelope.getOrderId())
                .idempotencyKey(fromOrderEnvelope.getIdempotencyKey())
                .build();

        InventoryResultEvent event = new InventoryResultEvent(
                env,
                items,
                null
        );

        send(env.getOrderId(), event, env.getEventId(), "STOCK_COMMITTED");
    }

    public void publishFailed(EventEnvelope fromOrderEnvelope,
                              java.util.List<OrderCreatedEvent.Item> items,
                              InventoryFailReason reason) {
        EventEnvelope env = EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OrderEventType.STOCK_FAILED)
                .occurredAt(Instant.now())
                .traceId(fromOrderEnvelope.getTraceId())
                .userId(fromOrderEnvelope.getUserId())
                .orderId(fromOrderEnvelope.getOrderId())
                .idempotencyKey(fromOrderEnvelope.getIdempotencyKey())
                .build();

        InventoryResultEvent event = new InventoryResultEvent(
                env,
                items,
                InventoryFailureReasonTranslator.toReasonCode(reason)
        );

        send(env.getOrderId(), event, env.getEventId(), "STOCK_FAILED");
    }

    private void send(Long orderId, InventoryResultEvent event, String eventId, String type) {
        String key = String.valueOf(orderId);
        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((res, ex) ->{
                    if(ex != null) {
                        log.error("Publish {} failed. orderId={}, eventId={}", type, orderId, eventId, ex);
                    } else {
                        log.info("Published {}. topic={}, partition={}, offset={}, orderId={}, eventId={}",
                                type,
                                res.getRecordMetadata().topic(),
                                res.getRecordMetadata().partition(),
                                res.getRecordMetadata().offset(),
                                orderId,
                                eventId);
                    }
                });
    }
}
