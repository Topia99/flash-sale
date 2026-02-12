package com.flashSale.order.service;

import com.flashSale.order.dto.CreateOrderRequest;
import com.flashSale.order.dto.OrderResponse;
import org.springframework.data.domain.Page;

public interface OrderService {
    OrderServiceImpl.CreateResult createOrder(Long userId, String idemKey, CreateOrderRequest req);
    OrderResponse getOrder(Long userId, Long id);
    Page<OrderResponse> listOrders(Long userId, String status, int page, int size, String sort);
    OrderResponse getByIdempotencyKey(Long userId, String idemKey);
}
