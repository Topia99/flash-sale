package com.flashSale.catalog.repo;

import com.flashSale.catalog.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByEvent_Id(Long eventId);
    List<Ticket> findByEvent_Id(Long eventId);
}
