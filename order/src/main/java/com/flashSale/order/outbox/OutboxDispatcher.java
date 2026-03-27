package com.flashSale.order.outbox;

import com.flashSale.order.events.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final OutboxEventLockRepository lockRepo;
    private final OutboxEventJpaRepository outboxRepo;
    private final OrderEventPublisher publisher;
    private final int batchSize = 50;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void dispatchOnce() {
        List<Long> ids = lockRepo.lockNextPendingIds(batchSize);
        if (ids.isEmpty()) return;

        List<OutboxEvent> events = outboxRepo.findAllById(ids);

        for (OutboxEvent ev : events) {
            try {
                // 这里发 kafka
                publisher.publishFromOutbox(ev.getId(), ev.getPayload());

                ev.setStatus(OutboxStatus.SENT);
                ev.setSentAt(Instant.now());
                ev.setLastError(null);
            } catch (Exception e) {
                ev.setRetryCount(ev.getRetryCount() + 1);
                ev.setLastError(shortError(e));

                // 你可以选择：
                // 1) 继续保持 PENDING 让它重试
                // 2) 重试超过阈值标 FAILED
                if(ev.getRetryCount() >= 10) {
                    ev.setStatus(OutboxStatus.FAILED);
                } else {
                    ev.setStatus(OutboxStatus.PENDING);
                }

                log.warn("Outbox send failed: id={}, type={}, retry={}",
                        ev.getId(), ev.getEventType(), ev.getRetryCount(), e);
            }
        }

        outboxRepo.saveAll(events);
    }

    private String shortError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) msg = e.getClass().getSimpleName();
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
