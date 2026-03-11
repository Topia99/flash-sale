package com.flashSale.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {
}
