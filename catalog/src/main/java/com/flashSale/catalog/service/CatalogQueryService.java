package com.flashSale.catalog.service;

import com.flashSale.catalog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CatalogQueryService {
    EventDetailResponse getEventDetail(Long eventId);
    Page<EventResponse> getPublishedEvents(Pageable pageable);
    TicketResponse getTicketById(Long id);
}
