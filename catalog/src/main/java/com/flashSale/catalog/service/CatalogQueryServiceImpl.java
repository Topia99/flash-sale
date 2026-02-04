package com.flashSale.catalog.service;

import com.flashSale.catalog.cache.CatalogCache;
import com.flashSale.catalog.domain.Event;
import com.flashSale.catalog.domain.EventStatus;
import com.flashSale.catalog.domain.Ticket;
import com.flashSale.catalog.dto.*;
import com.flashSale.catalog.exception.NotFoundException;
import com.flashSale.catalog.repo.EventRepository;
import com.flashSale.catalog.repo.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogQueryServiceImpl implements CatalogQueryService {
    private final EventRepository eventRepo;
    private final TicketRepository ticketRepo;

    private final CatalogCache cache;


    @Override
    @Transactional(readOnly = true)
    public EventDetailResponse getEventDetail(Long eventId) {
        String key = cache.eventDetailKey(eventId);

        // 1) 查缓存
        EventDetailResponse cached = cache.getJson(key, EventDetailResponse.class);

        if (cached != null) log.info("cache hit {}", key);
        else log.info("cache miss {}", key);

        if(cached != null) {
            return cached;
        }


        // 2) miss -> 查 DB（join fetch）
        Event event = eventRepo.findByIdWithTickets(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        // 3) 只返回可展示数据：PUBLISHED，否则 404（隐藏）
        if(event.getStatus() != EventStatus.PUBLISHED){
            throw new NotFoundException("Event not found");
        }

        // 4)  map to EventDetailResponse
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

        // 5) Save to cache
        cache.setJson(key, resp, cache.ttlWithJitter());

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
        String key = cache.ticketKey(id);

        // 1) 查缓存
        TicketResponse cached = cache.getJson(key, TicketResponse.class);

        if(cached != null){
            log.info("cache hit key:" + key);
        } else {
            log.info("cache miss key:" + key);
        }

        if(cached != null){
            return cached;
        }

        // 2) miss -> 查 DB（join fetch）
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        // 3)  map to EventDetailResponse
        TicketResponse resp = new TicketResponse(
                t.getId(),
                t.getEvent().getId(),
                t.getName(),
                t.getPrice(),
                t.getPerUserLimit(),
                t.getSaleStartsAt(),
                t.getSaleEndsAt(),
                t.getStatus().name()
        );

        // 5) Save to cache
        cache.setJson(key, resp, cache.ttlWithJitter());

        return resp;
    }
}
