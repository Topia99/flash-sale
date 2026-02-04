package com.flashSale.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TicketCreateRequest {

    @NotBlank(message = "name is required")
    @Size(max = 64, message = "name max length is 64")
    private String name;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "price must be >= 0")
    private BigDecimal price;

    // null = 不限购
    @Min(value = 1, message = "perUserLimit must be >= 1")
    private Integer perUserLimit;

    private Instant saleStartsAt;
    private Instant saleEndsAt;

    // 你也可以不让前端传 status，统一 OFF_SHELF
    private String status;
}
