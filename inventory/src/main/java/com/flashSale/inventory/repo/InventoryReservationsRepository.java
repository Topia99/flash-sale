package com.flashSale.inventory.repo;

import com.flashSale.inventory.domain.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryReservationsRepository extends JpaRepository<InventoryReservation, String> {
    Optional<InventoryReservation> findByTicketId(Long ticketId);
}
