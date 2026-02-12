package com.flashSale.order.controller;

import com.flashSale.order.dto.CreateOrderRequest;
import com.flashSale.order.dto.OrderResponse;
import com.flashSale.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid CreateOrderRequest req,
            @RequestHeader("X-User-Id") Long userId
    ) {
        var result = orderService.createOrder(userId, idempotencyKey, req);
        if(result.created()) {
            return ResponseEntity.status(201).body(result.response());
        }
        return ResponseEntity.ok(result.response());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long userId)
    {
        return ResponseEntity.status(200).body(orderService.getOrder(userId, id));
    }

    @GetMapping()
    public Page<OrderResponse> getOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        return orderService.listOrders(userId, status, page, size, sort);
    }

    @GetMapping("admin")
    public ResponseEntity<OrderResponse> getOrderByUserIdAndIdem(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.status(200).body(orderService.getByIdempotencyKey(userId, idempotencyKey));
    }

}
