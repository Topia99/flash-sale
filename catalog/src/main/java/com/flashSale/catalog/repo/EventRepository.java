package com.flashSale.catalog.repo;

import com.flashSale.catalog.domain.Event;
import com.flashSale.catalog.domain.EventStatus;
import com.flashSale.catalog.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("""
            select distinct e from Event e
            left join fetch e.tickets t
            where e.id = :id
            """)
    Optional<Event> findByIdWithTickets(@Param("id") Long id);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);
}
