package com.flashSale.order.service;

import com.flashSale.order.client.catalog.CatalogClient;
import com.flashSale.order.client.inventory.InventoryClient;
import com.flashSale.order.domain.*;
import com.flashSale.order.dto.*;
import com.flashSale.order.events.OrderCreatedEvent;
import com.flashSale.order.events.OrderEventPublisher;
import com.flashSale.order.exception.*;
import com.flashSale.order.repository.IdempotencyKeyRepository;
import com.flashSale.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{
    private final OrderRepository orderRepo;
    private final IdempotencyKeyRepository idemRepo;
    private final JdbcTemplate jdbcTemplate;
    private final InventoryClient inventoryClient;
    private final CatalogClient catalogClient;
    private final OrderEventPublisher publisher;

    /** 返回值里带 created=true/false 用于 controller 决定 201 or 200 */
    public record CreateResult(OrderResponse response, boolean created) {}

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, String idemKey, CreateOrderRequest req) {
       if(idemKey == null || idemKey.isBlank()) {
          throw new BadRequestException("Missing Idempotency-Key");
       }

        // currency null-safe (avoid NPE)
        String currency = (req == null || req.currency() == null || req.currency().isBlank())
                ? "USD"
                : req.currency().trim().toUpperCase();


        // 0) normalize items (合并重复 ticketId)
        List<OrderCreatedEvent.Item> items = normalizeItems(req);

        // 1) 先尝试插入 idempotency_keys (IN_PROGRESS)
        try {
            tryInsertIdemInProgress(userId, idemKey);
        } catch (DataIntegrityViolationException dup) {
            // 已存在同 user + idemKey，直接返回已有订单状态（不再发事件）
            IdempotencyKey existing = idemRepo.findByUserIdAndIdempotencyKey(userId, idemKey)
                    .orElseThrow(() -> dup);

            Long orderId = existing.getOrderId();
            if(orderId == null) {
                // 极少见：idemKey 插入成功但 orderId 还没绑定（并发窗口）
                // 这里返回 PROCESSING 比较合理
               return orderRepo.findByUserIdAndIdempotencyKey(userId, idemKey)
                       .map(o -> new OrderResponse(o.getId(), o.getStatus(), o.getFailureReason()))
                       .orElseThrow(() -> new RequestInProgressException("Order request is still in progress"));
            }

            Order order = orderRepo.findById(existing.getOrderId())
                    .orElseThrow(() -> {
                        log.error("Idem anomaly: COMPLETED but order not found. userId={}, idemKey={}, orderId={}",
                                userId, idemKey, orderId);
                        return new IllegalStateException("IdemKey bound to missing order");
                    });

            return new OrderResponse(order.getId(), order.getStatus(), order.getFailureReason());
        }

        // 2) 创建订单 (PROCESSING)
        Instant now = Instant.now();
        Order order = Order.builder()
                .userId(userId)
                .idempotencyKey(idemKey)
                .status(OrderStatus.PROCESSING)
                .currency(currency)
                .createdAt(now)
                .failureReason(null)
                .build();

        order = orderRepo.saveAndFlush(order);

        // 3) 绑定 idemKey -> orderId (让重试可以拿到同一个 orderId）
        idemRepo.bindOrderIfEmpty(userId, idemKey, order.getId());

        // 4) 添加OrderItems
        FailureReason getPriceFailure = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> pricedItems = new ArrayList<>();

        for(OrderCreatedEvent.Item it : items) {
            try {
                TicketResponse ticket = catalogClient.getTicket(it.ticketId());

                // Create OrderItem
                OrderItem item = new OrderItem();
                item.setTicketId(it.ticketId());
                item.setQty(it.qty());
                item.setUnitPrice(ticket.getPrice());

                BigDecimal lineAmount = ticket.getPrice().multiply(BigDecimal.valueOf(it.qty()));
                totalAmount = totalAmount.add(lineAmount);

                pricedItems.add(item);
            } catch (TicketNotFoundException e) {
                getPriceFailure = FailureReason.INVALID_REQUEST;
                log.warn("Catalog Ticket not found: ticketId={}", it.ticketId());
                break;
            } catch (CatalogErrorException e) {
                getPriceFailure = FailureReason.CATALOG_ERROR;
                log.warn("Catalog error: ticketId={}", it.ticketId());
                break;
            } catch(CatalogTimeoutException e) {
                getPriceFailure = FailureReason.CATALOG_TIMEOUT;
                log.warn("Catalog timeout: ticketId={}", it.ticketId(), e);
                break;
            } catch (Exception e) {
                getPriceFailure = FailureReason.CATALOG_ERROR;
                log.warn("Catalog Unkonwn error: ticketId={}", it.ticketId(), e);
                break;
            }
        }

        if(getPriceFailure != null) {
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason(getPriceFailure);
            order = orderRepo.saveAndFlush(order);


            // 按 failureReason 抛对应异常（或统一抛一个但带 reason）
            switch (getPriceFailure) {
                case INVALID_REQUEST -> throw new BadRequestException("Invalid ticketId");
                case CATALOG_TIMEOUT -> throw new CatalogTimeoutException("Catalog timeout");
                default -> throw new CatalogErrorException("Catalog error");
            }
        }

        // 5) 添加item with unit price, 更新order
        order.setTotalAmount(totalAmount);
        pricedItems.forEach(order::addItem);
        order = orderRepo.saveAndFlush(order);



        // 6) publish OrderCreated
        publisher.publishOrderCreated(order.getId(), userId, idemKey, null, items);

        // 7) 返回 202 语义（controller 负责用 accepted）
        return new OrderResponse(order.getId(), OrderStatus.PROCESSING, null);

    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BadRequestException("orderId must be positive");
        }

        Order order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order Not Found"));

        return toOrderDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(Long userId, String status, int page, int size, String sort) {
        size = Math.min(size, 100); // protect

        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orders;
        if(status == null || status.isBlank()) {
            orders = orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            OrderStatus st;
            try {
                st = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status + " (allowed: PENDING, CONFIRMED, FAILED)");

            }
            orders = orderRepo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, st, pageable);
        }

        return orders.map(order ->
            new OrderResponse(order.getId(), order.getStatus(), order.getFailureReason())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByIdempotencyKey(Long userId, String idemKey) {
        if (idemKey == null || idemKey.isBlank()) {
            throw new BadRequestException("Missing Idempotency-Key");
        }
        IdempotencyKey idem = idemRepo.findByUserIdAndIdempotencyKey(userId, idemKey)
                .orElseThrow(() -> new NotFoundException("Idempotency key not found"));

        if (idem.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new RequestInProgressException("Order in progress");
        }

        Long orderId = idem.getOrderId();

        if (orderId == null) {
            // 理论不应该发生：COMPLETED 但没 orderId
            log.error("Idem anomaly: COMPLETED but orderId is null. userId={}, idemKey={}", userId, idemKey);
            return orderRepo.findByUserIdAndIdempotencyKey(userId, idemKey)
                    .map(o -> new OrderResponse(o.getId(), o.getStatus(), o.getFailureReason()))
                    .orElseThrow(() -> new IllegalStateException("IdemKey bound to missing order"));
        }

        Order order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> {
                    log.error("Idem anomaly: COMPLETED but order not found. userId={}, idemKey={}, orderId={}",
                            userId, idemKey, orderId);
                    return new IllegalStateException("IdemKey bound to missing order");
                });

        return new OrderResponse(order.getId(), order.getStatus(), order.getFailureReason());
    }



    private List<OrderCreatedEvent.Item> normalizeItems(CreateOrderRequest req) {
        if(req.items() == null || req.items().isEmpty()) {
            throw new BadRequestException("items is required");
        }

        // 禁止重复 ticketId（更简单）
        Map<Long, Integer> agg = new HashMap<>();
        for(CreateOrderRequest.Item it : req.items()) {
            if(it == null || it.ticketId() == null || it.qty() == null || it.qty() <= 0) {
                throw new BadRequestException("invalid item");
            }
            agg.merge(it.ticketId(), it.qty(), Integer::sum);
        }

        return agg.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new OrderCreatedEvent.Item(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private void tryInsertIdemInProgress(Long userId, String idemKey) {
        if(idemKey == null || idemKey.isBlank()){
            throw new IllegalArgumentException("Idempotency Key is null or blank");
        }

        String sql = """
                INSERT INTO idempotency_keys
                    (user_id, idempotency_key, status, order_id)
                VALUES
                    (?, ?, 'IN_PROGRESS', NULL)
                """;
        jdbcTemplate.update(sql, userId, idemKey);
    }

    private OrderDetailResponse toOrderDetailResponse(Order order) {
        if(order == null) {
            throw new NotFoundException("Order Not Found");
        }

        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .failureReason(order.getFailureReason())
                .currency(order.getCurrency())
                .totalAmount(order.getTotalAmount())
                .idempotencyKey(order.getIdempotencyKey())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getOrderItems()
                        .stream()
                        .map(ItemDto::new)
                        .toList())
                .build();
    }
}
