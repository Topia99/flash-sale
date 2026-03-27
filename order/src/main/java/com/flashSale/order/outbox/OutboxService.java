package com.flashSale.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventJpaRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public void enqueueOrderCreated(String aggregateType, Long aggregateId, String eventType,  Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent ev = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxRepo.save(ev);

        } catch (Exception e) {
            // 这里抛异常 -> 会让当前事务回滚
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
