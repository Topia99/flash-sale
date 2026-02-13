package com.flashSale.order.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(Long orderId,
                                    Long userId,
                                    String idempotencyKey,
                                    String traceId,
                                    List<OrderCreatedEvent.Item> items) {

        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OrderEventType.ORDER_CREATED)
                .occurredAt(Instant.now())
                .traceId(traceId)  // 允许为null，后面接入tracing再补
                .orderId(orderId)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .envelope(envelope)
                .items(items)
                .build();

        String key = String.valueOf(orderId);

        kafkaTemplate.send(OrderTopics.ORDER_CREATED_V1, key, event)
                .whenComplete((result, ex) ->{
                    if(ex != null) {
                        log.error("Kafka publish ORDER_CREATED failed. orderId={}, idemKey={}, eventId={}",
                                orderId, idempotencyKey, envelope.getEventId(), ex);
                    } else {
                        log.info("Kafka published ORDER_CREATED. topic={}, partition={}, offset={}, orderId={}, eventId={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                orderId,
                                envelope.getEventId());
                    }
                });
    }
}
