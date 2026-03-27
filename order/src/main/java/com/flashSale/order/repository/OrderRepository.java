package com.flashSale.order.repository;

import com.flashSale.order.domain.FailureReason;
import com.flashSale.order.domain.Order;
import com.flashSale.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);
    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Modifying
    @Query("""
            update Order o
            set o.status = :newStatus, o.updatedAt = CURRENT_TIMESTAMP, o.failureReason = :reason
            where o.id = :orderId and o.status = :expectedStatus
            """)
    int updateStatusIfCurrent(@Param("orderId") Long orderId,
                              @Param("expectedStatus") OrderStatus expectedStatus,
                              @Param("newStatus") OrderStatus newStatus,
                              @Param("reason") FailureReason reason);

}
