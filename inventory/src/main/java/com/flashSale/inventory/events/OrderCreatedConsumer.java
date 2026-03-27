package com.flashSale.inventory.events;


import com.flashSale.inventory.saga.InventorySagaResult;
import com.flashSale.inventory.saga.InventorySagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {
    private static final String TOPIC = "order.created.v1";

    private final InventoryResultPublisher resultPublisher;
    private final InventorySagaService sagaService;

    @KafkaListener(topics = TOPIC, groupId = "inventory-reserve-g1")
    public void onMessage(ConsumerRecord<String, OrderCreatedEvent> record,
                          Acknowledgment ack) {

        String key = record.key();
        OrderCreatedEvent event = record.value();

        // 防御式：避免空值导致 NPE
        if(event == null || event.getEnvelope() == null) {
            log.warn("Received null/invalid OrderCreatedEvent. key={}, partition={}, offset={}",
                    key, record.partition(), record.offset());
            ack.acknowledge(); // 这类坏消息也要ack，避免无限重试卡死
            return;
        }

        EventEnvelope env = event.getEnvelope();

        log.info("Inventory handling ORDER_CREATED. key={}, partition={}, offset={}, eventId={}, orderId={}, userId={}, idemKey={}, items={}",
                key,
                record.partition(),
                record.offset(),
                env.getEventId(),
                env.getOrderId(),
                env.getUserId(),
                env.getIdempotencyKey(),
                event.getItems()
        );

        // 1) 处理整单（reserve/commit/release）
        InventorySagaResult result = sagaService.handleOrderCreated(event);

        // 2) 发结果事件
        if(result.eventType() == OrderEventType.STOCK_COMMITTED) {
            resultPublisher.publishCommitted(env, event.getItems());
        } else {
            resultPublisher.publishFailed(env, event.getItems(), result.reason());
        }

        // Shadow Mode: 先不扣库存，只确认消费成功
        ack.acknowledge();
    }
}
