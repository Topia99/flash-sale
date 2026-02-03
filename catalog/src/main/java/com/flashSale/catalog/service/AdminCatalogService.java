package com.flashSale.catalog.service;

import com.flashSale.catalog.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminCatalogService {
    EventResponse createEvent(EventCreateRequest req);
    EventResponse updateEvent(Long eventId, EventUpdateRequest req);

    Page<EventResponse> listEvents(String status, int page, int size, String sort);

    EventDetailResponse getEventDetail(Long eventId);

    EventResponse publishEvent(Long eventId);
    EventResponse unpublishEvent(Long eventId);
    EventResponse endEvent(Long eventId);

    TicketResponse createTicket(Long eventId, TicketCreateRequest req);
    TicketResponse updateTicket(Long ticketId, TicketUpdateRequest req);

    List<TicketResponse> listTicketsByEvent(Long eventId);
    TicketResponse getTicket(Long ticketId);

    TicketResponse ticketOnSale(Long ticketId);
    TicketResponse ticketOffShelf(Long ticketId);
}
