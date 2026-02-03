package com.flashSale.catalog.service;

import com.flashSale.catalog.domain.Event;
import com.flashSale.catalog.domain.EventStatus;
import com.flashSale.catalog.domain.Ticket;
import com.flashSale.catalog.domain.TicketStatus;
import com.flashSale.catalog.dto.*;
import com.flashSale.catalog.exception.BadRequestException;
import com.flashSale.catalog.exception.ConflictException;
import com.flashSale.catalog.exception.NotFoundException;
import com.flashSale.catalog.repo.EventRepository;
import com.flashSale.catalog.repo.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


@Service
public class CatalogQueryServiceImpl implements CatalogQueryService {
    private final EventRepository eventRepo;
    private final TicketRepository ticketRepo;

    public CatalogQueryServiceImpl(EventRepository eventRepo,
                                   TicketRepository ticketRepo) {
        this.eventRepo = eventRepo;
        this.ticketRepo = ticketRepo;
    }

    @Override
    public EventDetailResponse getEventDetail(Long eventId) {
        Event event = eventRepo.findByIdWithTickets(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if(event.getStatus() != EventStatus.PUBLISHED){
            throw new NotFoundException("Event not found");
        }

        EventDetailResponse resp = new EventDetailResponse();
        resp.setId(event.getId());
        resp.setTitle(event.getTitle());
        resp.setDescription(event.getDescription());
        resp.setStartsAt(event.getStartsAt());
        resp.setEndsAt(event.getEndsAt());
        resp.setStatus(event.getStatus().name());

        Long eid = event.getId();
        List<TicketResponse> tickets = event.getTickets().stream()
                .sorted(java.util.Comparator.comparing(Ticket::getId))
                .map(t -> new TicketResponse(
                        t.getId(),
                        eid,
                        t.getName(),
                        t.getPrice(),
                        t.getPerUserLimit(),
                        t.getSaleStartsAt(),
                        t.getSaleEndsAt(),
                        t.getStatus().name()
                ))
                .toList();

        resp.setTickets(tickets);
        return resp;
    }

    public Page<EventResponse> getPublishedEvents(Pageable pageable) {
        return eventRepo.findByStatus(EventStatus.PUBLISHED, pageable)
                .map(e -> new EventResponse(
                        e.getId(),
                        e.getTitle(),
                        e.getDescription(),
                        e.getStartsAt(),
                        e.getEndsAt(),
                        e.getStatus().name()
                ));
    }

    @Override
    public TicketResponse getTicketById(Long id) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        return new TicketResponse(
            t.getId(),
            t.getEvent().getId(),
            t.getName(),
            t.getPrice(),
            t.getPerUserLimit(),
            t.getSaleStartsAt(),
            t.getSaleEndsAt(),
            t.getStatus().name()
        );
    }
}
