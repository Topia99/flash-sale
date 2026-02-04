package com.flashSale.catalog.controller;

import com.flashSale.catalog.dto.*;
import com.flashSale.catalog.service.CatalogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogQueryService catalogService;

    @GetMapping("/events/{id}")
    public EventDetailResponse getEventDetail(@PathVariable("id") Long id){
        return catalogService.getEventDetail(id);
    }

    @GetMapping("/events")
    public Page<EventResponse> getPublishedEvent(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "startsAt")
        );
        return catalogService.getPublishedEvents(pageable);
    }

    @GetMapping("/tickets/{id}")
    public TicketResponse getTicketById(@PathVariable("id") Long id){
        return catalogService.getTicketById(id);
    }
}
