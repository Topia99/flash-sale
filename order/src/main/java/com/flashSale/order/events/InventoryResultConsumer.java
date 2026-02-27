package com.flashSale.order.events;

import com.flashSale.order.domain.FailureReason;
import com.flashSale.order.domain.FailureReasonParser;
import com.flashSale.order.domain.IdempotencyStatus;
import com.flashSale.order.domain.OrderStatus;
import com.flashSale.order.repository.IdempotencyKeyRepository;
import com.flashSale.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryResultConsumer {

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idemRepository;

    @KafkaListener(topics = OrderTopics.INVENTORY_RESULT_V1, groupId = "order-inventory-result-g1")
    @Transactional
    public void onMessage(ConsumerRecord<String, InventoryResultEvent> record,
                          Acknowledgment ack) {

        InventoryResultEvent event = record.value();
        if (event == null || event.getEnvelope() == null) {
            log.warn("Invalid InventoryResultEvent. key={}, partition={}, offset={}",
                    record.key(), record.partition(), record.offset());
            ack.acknowledge();
            return;
        }

        EventEnvelope env = event.getEnvelope();
        Long orderId = env.getOrderId();

        OrderStatus target;
        FailureReason reason = null;

        if(env.getEventType() == OrderEventType.STOCK_COMMITTED) {
            target = OrderStatus.CONFIRMED;
        } else if (env.getEventType() == OrderEventType.STOCK_FAILED) {
            target = OrderStatus.FAILED;
            reason = FailureReasonParser.parseOrDefault(event.getReasonCode());

        } else {
            ack.acknowledge();
            return;
        }

        // 幂等：只允许 PROCESSING -> 终态
        int updated = orderRepository.updateStatusIfCurrent(orderId, OrderStatus.PROCESSING, target, reason);

        if (updated == 1) {
            // 同步更新 idem 状态为 COMPLETED（方便同 key 重试返回最终态）
            idemRepository.updateStatus(env.getUserId(), env.getIdempotencyKey(), IdempotencyStatus.COMPLETED);
            log.info("Order finalized. orderId={}, status={}, reason={}", orderId, target, reason);
        } else {
            log.info("Ignore duplicate/late result. orderId={}, eventType={}, eventId={}",
                    orderId, env.getEventType(), env.getEventId());
        }

        ack.acknowledge();
    }
}
