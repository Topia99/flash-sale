package com.flashSale.order.dto;

import com.flashSale.order.domain.FailureReason;
import com.flashSale.order.domain.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class OrderDetailResponse {
    private Long orderId;
    private Long userId;
    private OrderStatus status;
    private FailureReason failureReason;
    private String currency;
    private BigDecimal totalAmount;
    private String idempotencyKey;
    private List<ItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;

}

