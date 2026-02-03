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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCatalogServiceImpl implements AdminCatalogService{
    private final EventRepository eventRepo;
    private final TicketRepository ticketRepo;

    // ===== Event =====

    @Override
    @Transactional
    public EventResponse createEvent(EventCreateRequest eventRequest){
        if (!eventRequest.getEndsAt().isAfter(eventRequest.getStartsAt())) {
            throw new BadRequestException("endsAt must be after startsAt");
        }

        var event = new Event();
        event.setTitle(eventRequest.getTitle());
        event.setDescription(eventRequest.getDescription());
        event.setStartsAt(eventRequest.getStartsAt());
        event.setEndsAt(eventRequest.getEndsAt());
        event.setStatus(EventStatus.DRAFT);

        Event saved = eventRepo.save(event);

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getStatus().name());
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long eventId, EventUpdateRequest req) {
        validateEventTime(req.getStartsAt(), req.getEndsAt());

        Event e = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        // 建议：仅 DRAFT 可改核心字段（最简单、最安全）
        if (e.getStatus() != EventStatus.DRAFT) {
            throw new ConflictException("Only DRAFT event can be updated");
        }

        if (req.getTitle() != null) {
            String title = req.getTitle().trim();
            if (title.isEmpty()) throw new BadRequestException("title cannot be blank");
            if (title.length() > 128) throw new BadRequestException("title max length is 128");
            e.setTitle(title);
        }

        if (req.getDescription() != null) {
            // description 允许为空字符串，看你策略
            e.setDescription(req.getDescription());
        }

        // 合并后的时间（很重要）
        Instant newStarts = (req.getStartsAt() != null) ? req.getStartsAt() : e.getStartsAt();
        Instant newEnds   = (req.getEndsAt()   != null) ? req.getEndsAt()   : e.getEndsAt();

        // 只有当用户传了任意一个时间字段时才校验（也可以每次都校验）
        if (req.getStartsAt() != null || req.getEndsAt() != null) {
            if (newStarts == null || newEnds == null || !newEnds.isAfter(newStarts)) {
                throw new BadRequestException("endsAt must be after startsAt");
            }
            e.setStartsAt(newStarts);
            e.setEndsAt(newEnds);
        }

        Event saved = eventRepo.save(e);

        // cache delete: event:{id}:detail （后面加 Redis）
        return new EventResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getStartsAt(),
                saved.getEndsAt(),
                saved.getStatus().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> listEvents(String status, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        Page<Event> result;
        if (status == null || status.isBlank()) {
            result = eventRepo.findAll(pageable);
        } else {
            EventStatus st;
            try {
                st = EventStatus.valueOf(status.trim());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid event status: " + status);
            }
            result = eventRepo.findByStatus(st, pageable);
        }

        return result.map(this::toEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailResponse getEventDetail(Long eventId) {
        Event e = eventRepo.findByIdWithTickets(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        List<TicketResponse> tickets = e.getTickets().stream()
                .sorted(Comparator.comparing(Ticket::getId))
                .map(t -> toTicketResponse(t, e.getId()))
                .toList();

        return new EventDetailResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getStatus().name(),
                tickets
        );
    }

    @Override
    @Transactional
    public EventResponse publishEvent(Long eventId) {
        Event e = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (e.getStatus() != EventStatus.DRAFT) {
            throw new ConflictException("Only DRAFT event can be published");
        }
        validateEventTime(e.getStartsAt(), e.getEndsAt());

        // 必须至少有一张 ticket
        if (!ticketRepo.existsByEvent_Id(eventId)) {
            throw new ConflictException("Cannot publish event without tickets");
        }

        e.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepo.save(e);

        // TODO: cache delete: event:{id}:detail
        return toEventResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse unpublishEvent(Long eventId) {
        Event e = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (e.getStatus() != EventStatus.PUBLISHED) {
            throw new ConflictException("Only PUBLISHED event can be unpublished");
        }

        e.setStatus(EventStatus.DRAFT);
        Event saved = eventRepo.save(e);

        // TODO: cache delete: event:{id}:detail
        return toEventResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse endEvent(Long eventId) {
        Event e = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (e.getStatus() == EventStatus.ENDED) {
            return toEventResponse(e); // 幂等
        }

        e.setStatus(EventStatus.ENDED);
        Event saved = eventRepo.save(e);

        // TODO: cache delete: event:{id}:detail
        return toEventResponse(saved);
    }

    // ===== Ticket =====


    @Override
    @Transactional
    public TicketResponse createTicket(Long eventId, TicketCreateRequest req) {
        // Check if eventId exist
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        //Only DRAFT can add ticket
        if(event.getStatus() != EventStatus.DRAFT){
            throw new ConflictException("Only DRAFT event can add tickets");
        }

        Instant s = req.getSaleStartsAt();
        Instant e = req.getSaleEndsAt();
        if(s != null && e != null && !e.isAfter(s)){
            throw new ConflictException("saleEndsAt must be after saleStartsAt");
        }

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setName(req.getName().trim());
        ticket.setPrice(req.getPrice());
        ticket.setPerUserLimit(req.getPerUserLimit());
        ticket.setSaleStartsAt(req.getSaleStartsAt());
        ticket.setSaleEndsAt(req.getSaleEndsAt());

        if (req.getStatus() == null || req.getStatus().isBlank()) {
            ticket.setStatus(TicketStatus.OFF_SHELF);
        } else {
            try {
                ticket.setStatus(TicketStatus.valueOf(req.getStatus().trim()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid ticket status: " + req.getStatus());
            }
        }

        Ticket saved = ticketRepo.save(ticket);

        // TODO: cache delete: event:{id}:detail
        return new TicketResponse(
                saved.getId(),
                event.getId(),
                saved.getName(),
                saved.getPrice(),
                saved.getPerUserLimit(),
                saved.getSaleStartsAt(),
                saved.getSaleEndsAt(),
                saved.getStatus().name()
        );
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long ticketId, TicketUpdateRequest req) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        if(req.getName() != null){
            String name = req.getName().trim();
            if(name.isEmpty()){
                throw new BadRequestException("Name can not be blank");
            }
            t.setName(name);
        }

        if(req.getPrice() != null){
            t.setPrice(req.getPrice());
        }

        if(req.getPerUserLimit() != null){
            t.setPerUserLimit(req.getPerUserLimit());
        }

        Instant start = (req.getSaleStartsAt() != null) ? req.getSaleStartsAt() : t.getSaleStartsAt();
        Instant end = (req.getSaleEndsAt() != null) ? req.getSaleEndsAt() : t.getSaleEndsAt();

        if(req.getSaleStartsAt() != null || req.getSaleEndsAt() != null){
            if(start == null || end == null || start.isAfter(end)){
                throw new BadRequestException("End time must after start time");
            }
            t.setSaleStartsAt(start);
            t.setSaleEndsAt(end);
        }

        Ticket saved = ticketRepo.save(t);

        Long eventId = saved.getEvent().getId();
        // TODO: cache delete: event:{id}:detail
        return toTicketResponse(saved, eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listTicketsByEvent(Long eventId) {
        // event 不存在也应该 404（更友好）
        if (!eventRepo.existsById(eventId)) {
            throw new NotFoundException("Event not found");
        }

        return ticketRepo.findByEvent_Id(eventId).stream()
                .sorted(Comparator.comparing(Ticket::getId))
                .map(t -> toTicketResponse(t, eventId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long ticketId) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        return toTicketResponse(t, t.getEvent().getId());
    }

    @Override
    @Transactional
    public TicketResponse ticketOnSale(Long ticketId) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        t.setStatus(TicketStatus.ON_SALE);
        Ticket saved = ticketRepo.save(t);

        Long eventId = saved.getEvent().getId();
        // TODO: cache delete
        return toTicketResponse(saved, eventId);
    }

    @Override
    @Transactional
    public TicketResponse ticketOffShelf(Long ticketId) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        t.setStatus(TicketStatus.OFF_SHELF);
        Ticket saved = ticketRepo.save(t);

        Long eventId = saved.getEvent().getId();
        // TODO: cache delete
        return toTicketResponse(saved, eventId);
    }


    // ===== Helpers =====

    private void validateEventTime(Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new BadRequestException("endsAt must be after startsAt");
        }
    }

    private void validateSaleWindow(Instant saleStartsAt, Instant saleEndsAt) {
        if (saleStartsAt != null && saleEndsAt != null && !saleEndsAt.isAfter(saleStartsAt)) {
            throw new BadRequestException("saleEndsAt must be after saleStartsAt");
        }
    }

    private Sort parseSort(String sort) {
        // 支持 sort=startsAt,asc / sort=startsAt,desc / sort=createdAt,desc
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "startsAt");
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1])) ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    private TicketStatus parseTicketStatusOrDefault(String status, TicketStatus def) {
        if (status == null || status.isBlank()) return def;
        try {
            return TicketStatus.valueOf(status.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid ticket status: " + status);
        }
    }

    private EventResponse toEventResponse(Event e) {
        return new EventResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getStatus().name()
        );
    }

    private TicketResponse toTicketResponse(Ticket t, Long eventId) {
        return new TicketResponse(
                t.getId(),
                eventId,
                t.getName(),
                t.getPrice(),
                t.getPerUserLimit(),
                t.getSaleStartsAt(),
                t.getSaleEndsAt(),
                t.getStatus().name()
        );
    }
}
