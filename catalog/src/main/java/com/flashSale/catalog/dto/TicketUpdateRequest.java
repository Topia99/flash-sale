package com.flashSale.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
public class TicketUpdateRequest {
    @Size(max = 64, message = "name max length is 64")
    private String name;

    @DecimalMin(value = "0.00", inclusive = true, message = "price must be >= 0")
    private BigDecimal price;

    // null = 不限购
    @Min(value = 1, message = "perUserLimit must be >= 1")
    private Integer perUserLimit;

    private Instant saleStartsAt;
    private Instant saleEndsAt;
}
