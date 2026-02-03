package com.flashSale.catalog.controller;

import com.flashSale.catalog.dto.*;
import com.flashSale.catalog.service.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog/admin")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final AdminCatalogService adminService;

    // ===== Event =====

    @PostMapping("/events")
    public EventResponse createEvent(@Valid @RequestBody EventCreateRequest req) {
        return adminService.createEvent(req);
    }

    @PutMapping("/events/{id}")
    public EventResponse updateEvent(@PathVariable(name = "id") Long id,
                                     @Valid @RequestBody EventUpdateRequest req) {
        return adminService.updateEvent(id, req);
    }

    @GetMapping("/events")
    public Page<EventResponse> listEvents(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        return adminService.listEvents(status, page, size, sort);
    }

    @GetMapping("/events/{id}")
    public EventDetailResponse getEventDetail(@PathVariable(name = "id") Long id) {
        return adminService.getEventDetail(id);
    }

    @PostMapping("/events/{id}/publish")
    public EventResponse publish(@PathVariable(name = "id") Long id) {
        return adminService.publishEvent(id);
    }

    @PostMapping("/events/{id}/unpublish")
    public EventResponse unpublish(@PathVariable(name = "id") Long id) {
        return adminService.unpublishEvent(id);
    }

    @PostMapping("/events/{id}/end")
    public EventResponse end(@PathVariable(name = "id") Long id) {
        return adminService.endEvent(id);
    }

    // ===== Ticket =====

    @PostMapping("/events/{id}/tickets")
    public TicketResponse createTicket(@PathVariable(name = "id") Long id,
                                       @Valid @RequestBody TicketCreateRequest req) {
        return adminService.createTicket(id, req);
    }

    @GetMapping("/events/{id}/tickets")
    public List<TicketResponse> listTickets(@PathVariable(name = "id") Long id) {
        return adminService.listTicketsByEvent(id);
    }

    @GetMapping("/tickets/{ticketId}")
    public TicketResponse getTicket(@PathVariable(name = "ticketId") Long ticketId) {
        return adminService.getTicket(ticketId);
    }

    @PutMapping("/tickets/{ticketId}")
    public TicketResponse updateTicket(@PathVariable(name = "ticketId") Long ticketId,
                                       @Valid @RequestBody TicketUpdateRequest req) {
        return adminService.updateTicket(ticketId, req);
    }

    @PostMapping("/tickets/{ticketId}/on-sale")
    public TicketResponse onSale(@PathVariable(name = "ticketId") Long ticketId) {
        return adminService.ticketOnSale(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/off-shelf")
    public TicketResponse offShelf(@PathVariable(name = "ticketId") Long ticketId) {
        return adminService.ticketOffShelf(ticketId);
    }
}
