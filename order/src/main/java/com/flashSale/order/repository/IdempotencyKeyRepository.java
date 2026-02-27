package com.flashSale.order.repository;

import com.flashSale.order.domain.IdempotencyKey;
import com.flashSale.order.domain.IdempotencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Modifying
    @Query("""
            update IdempotencyKey i
            set i.status = :newStatus, i.updatedAt = CURRENT_TIMESTAMP
            WHERE i.userId = :userId and i.idempotencyKey = :idemKey
            """)
    int updateStatus(@Param("userId") Long userId,
                     @Param("idemKey") String idemKey,
                     @Param("newStatus")IdempotencyStatus newStatus);

    @Modifying
    @Query("""
            update IdempotencyKey i
            set i.orderId = :orderId, i.updatedAt = CURRENT_TIMESTAMP
            where i.userId = :userId and i.idempotencyKey = :idemKey and i.orderId is null
            """)
    int bindOrderIfEmpty(@Param("userId") Long userId,
                         @Param("idemKey") String idemKey,
                         @Param("orderId") Long orderId);
}
