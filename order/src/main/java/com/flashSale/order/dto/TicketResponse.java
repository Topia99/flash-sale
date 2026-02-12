package com.flashSale.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor
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
