package com.flashSale.catalog.dto;

import com.flashSale.catalog.domain.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class EventDetailResponse {
    private Long id;
    private String title;
    private String description;
    private Instant startsAt;
    private Instant endsAt;
    private String status;
    private List<TicketResponse> tickets;
}
