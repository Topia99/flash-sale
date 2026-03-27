package com.flashSale.order.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventLockRepository {
    private final JdbcTemplate jdbcTemplate;

    public List<Long> lockNextPendingIds(int batchSize) {
        // MySQL 8: FOR UPDATE SKIP LOCKED
        return jdbcTemplate.queryForList("""
                    SELECT id
                    FROM outbox_events
                    WHERE status = 'PENDING'
                    ORDER BY id
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                """, Long.class, batchSize);
    }
}
