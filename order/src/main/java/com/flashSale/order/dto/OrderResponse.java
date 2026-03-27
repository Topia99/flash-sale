package com.flashSale.order.dto;

import com.flashSale.order.domain.FailureReason;
import com.flashSale.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long orderId,
        OrderStatus status,
        FailureReason failReason
){}
