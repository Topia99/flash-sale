package com.flashSale.inventory.repo;

import com.flashSale.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Reserve atomic update:
     * available -= qty, reserved += qty
     * only if available >= qty
     *
     * @return rows affected (1 = success, 0 = insufficient or not found)
     */
    @Modifying // return how many rows affected. 1 row affected, return 1.
    @Transactional
    @Query(value = """
            UPDATE inventory
            SET available = available - :qty,
                reserved = reserved + :qty,
                version = version + 1
            WHERE ticket_id = :ticketId
                AND available >= :qty
            """, nativeQuery = true)
    int reserveAtomic(
            @Param("ticketId") long ticketId,
            @Param("qty") int qty);


    /**
     * Release Atomic update:
     * reserved -= qty, available += qty
     * only if reserved >= qty
     *
     * @return rows affected (1 = success, 0 = insufficient or not found)
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE inventory
            SET reserved = reserved - :qty,
                available = available + :qty,
                version = version + 1
            WHERE ticket_id = :ticketId
                AND reserved >= :qty
            """, nativeQuery = true)
    int releaseAtomic(
            @Param("ticketId") long ticketId,
            @Param("qty") int qty
    );

    /**
     * Commit Atomic update:
     * reserved -= qty, sold += qty
     * only if reserved >= qty
     *
     * @return rows affected (1 = success, 0 = insufficient or not found)
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE inventory
            SET reserved = reserved - :qty,
                sold = sold + :qty,
                version = version + 1
            WHERE ticket_id = :ticketId
                AND reserved >= :qty
            """, nativeQuery = true)
    int commitAtomic(
            @Param("ticketId") long ticketId,
            @Param("qty") int qty
    );
}
