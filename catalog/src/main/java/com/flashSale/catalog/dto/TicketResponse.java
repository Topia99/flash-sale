package com.flashSale.catalog.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TicketResponse {
    private Long id;
    private Long eventId;
    private String name;
    private BigDecimal price;
    private Integer perUserLimit;
    private Instant saleStartsAt;
    private Instant saleEndsAt;
    private String status;
}

