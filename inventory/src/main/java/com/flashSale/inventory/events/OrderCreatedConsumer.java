package com.flashSale.inventory.events;


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

        log.info("Received ORDER_CREATED. key={}, partition={}, offset={}, eventId={}, orderId={}, userId={}, idemKey={}, items={}",
                key,
                record.partition(),
                record.offset(),
                env.getEventId(),
                env.getOrderId(),
                env.getUserId(),
                env.getIdempotencyKey(),
                event.getItems()
        );

        // Shadow Mode: 先不扣库存，只确认消费成功
        ack.acknowledge();
    }
}
