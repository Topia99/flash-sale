package com.flashSale.order.service;

import com.flashSale.order.client.catalog.CatalogClient;
import com.flashSale.order.client.inventory.InventoryClient;
import com.flashSale.order.domain.*;
import com.flashSale.order.dto.CreateOrderRequest;
import com.flashSale.order.dto.OrderResponse;
import com.flashSale.order.dto.ReservationResponse;
import com.flashSale.order.dto.TicketResponse;
import com.flashSale.order.events.OrderCreatedEvent;
import com.flashSale.order.events.OrderEventPublisher;
import com.flashSale.order.exception.*;
import com.flashSale.order.repository.IdempotencyKeyRepository;
import com.flashSale.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

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
    public CreateResult createOrder(Long userId, String idemKey, CreateOrderRequest req) {
        if (idemKey == null || idemKey.isBlank()) {
            throw new BadRequestException("Missing Idempotency_Key");
        }

        // 1) 业务校验 & 归一化 items（禁止重复 ticketId 或合并）
        List<CreateOrderRequest.Item> normalized = normalizeItems(req);

        log.info("CreateOrder attempt: userId={}, idemKey={}, items={}, currency={}",
                userId, idemKey, normalized.size(),
                (req.currency() == null || req.currency().isBlank()) ? "USD" : req.currency());



        try {
            // 用custom jdbc template insert，防止flush时唯一性报错时 污染hibernate session
            tryInsertIdemInProgress(userId, idemKey);
            log.info("Idem lock acquired: userId={}, idemKey={} (FIRST_REQUEST)", userId, idemKey);
        } catch (DataIntegrityViolationException dup) {
            // 已存在同 (userId, idemKey)
            log.warn("Idem duplicate: userId={}, idemKey={} (retry/concurrent)", userId, idemKey);
            IdempotencyKey existing = idemRepo.findByUserIdAndIdempotencyKey(userId, idemKey)
                    .orElseThrow(() -> dup);

            if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                log.warn("Idem IN_PROGRESS: userId={}, idemKey={} -> 409", userId, idemKey);
                throw new RequestInProgressException();
            }

            // COMPLETED: 返回已创建订单（200）
            Long orderId = existing.getOrderId();
            if(orderId == null) {
                log.error("Idem anomaly: COMPLETED but orderId is null. userId={}, idemKey={}",
                        userId, existing.getIdempotencyKey());
                // 理论不该发生，保护一下
                throw new RequestInProgressException();
            }

            log.info("Idem COMPLETED: userId={}, idemKey={}, orderId={}, -> return existing (200)",
                    userId, idemKey, orderId);
            Order order = orderRepo.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> {
                        log.error("Idem anomaly: COMPLETED but order not found. userId={}, idemKey={}, orderId={}",
                                userId, idemKey, orderId);
                        return new RequestInProgressException();
                    });

            return new CreateResult(toResponse(order), false);
        }

        // 3)  Reserve all items (all-or-nothing)
        List<String> reservedIds = new ArrayList<>();
        FailureReason reserveFailure = null;

        for(CreateOrderRequest.Item it :  normalized) {
            String reservationId = idemKey + ":" + it.ticketId();
            try {
                inventoryClient.reserve(idemKey + ":" + it.ticketId(), it.ticketId(), it.qty());
                reservedIds.add(reservationId);
                log.info("Inventory RESERVED: reservationId={}, ticketId={}, qty={}", reservationId, it.ticketId(), it.qty());
            } catch (InsufficientStockException e) {
                reserveFailure = FailureReason.INSUFFICIENT_STOCK;
                log.warn("Inventory INSUFFICIENT_STOCK: reservationId={}, ticketId={}, qty={}", reservationId, it.ticketId(), it.qty());
                break;
            } catch (InventoryErrorException e) {
                reserveFailure = FailureReason.INVENTORY_ERROR;
                log.warn("Inventory ERROR: reservationId={}, ticketId={}, qty={}", reservationId, it.ticketId(), it.qty());
                break;
            } catch (InventoryTimeoutException e) {
                reserveFailure = FailureReason.INVENTORY_TIMEOUT;
                log.warn("Inventory TIMEOUT: reservationId={}, ticketId={}, qty={}", reservedIds, it.ticketId(), it.qty());
                break;
            } catch (TicketNotFoundException e) {
                reserveFailure = FailureReason.INVALID_REQUEST;
                log.warn("Inventory ticket not found: reservationId={}, ticketId={}, qty={}",
                        reservationId, it.ticketId(), it.qty());
                break;
            } catch (Exception e) {
                reserveFailure = FailureReason.INVENTORY_ERROR;
                log.error("Inventory UNKNOWN_ERROR: reservationId={}, ticketId={}, qty={}", reservationId, it.ticketId(), it.qty(), e);
                break;
            }
        }

        // 4) If reserve failed -> release already reserved, create FAILED order, mark idem completed, return
        if(reserveFailure != null) {
            releaseAll(reservedIds, "Reserve failed");

            Order failed = new Order();
            failed.setUserId(userId);
            failed.setStatus(OrderStatus.FAILED);
            failed.setFailureReason(reserveFailure);
            saidFailedCurrencyAndTotals(failed, req, normalized);
            failed.setIdempotencyKey(idemKey);

            // keep items for audit (recommended)
            for(CreateOrderRequest.Item it : normalized) {
                OrderItem item = new OrderItem();
                item.setTicketId(it.ticketId());
                item.setQty(it.qty());
                item.setUnitPrice(BigDecimal.ZERO);
                failed.addItem(item);
            }

            Order savedFailed = orderRepo.save(failed);
            log.info("Order FAILED created: userId={}, idemKey={}, orderId={}, reason={}",
                    userId, idemKey, savedFailed.getId(), reserveFailure);

            markIdemCompleted(userId, idemKey, savedFailed.getId());
            log.info("Idem marked COMPLETED (FAILED): userId={}, idemKey={}, orderId={}", userId, idemKey, savedFailed.getId());

            return new CreateResult(toResponse(savedFailed), true);
        }

        // 5) Reserved all succeed -> Fetch ticket price from Catalog and Create order PENDING
        FailureReason getPriceFailure = null;
        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = new Order();
        order.setUserId(userId);
        order.setCurrency(req.currency() == null || req.currency().isBlank() ? "USD" : req.currency());
        order.setIdempotencyKey(idemKey);

        // 用临时列表收集 items，避免失败时出现“半加半不加”
        List<OrderItem> pricedItems = new ArrayList<>();

        for(CreateOrderRequest.Item it : normalized) {
            try{
                TicketResponse ticket = catalogClient.getTicket(it.ticketId());

                // Create OrderItem
                OrderItem item = new OrderItem();
                item.setTicketId((it.ticketId()));
                item.setQty(it.qty());
                item.setUnitPrice(ticket.getPrice());

                BigDecimal lineAmount = ticket.getPrice().multiply(BigDecimal.valueOf(it.qty()));
                totalAmount = totalAmount.add(lineAmount);

                pricedItems.add(item);
            } catch (TicketNotFoundException e) {
                getPriceFailure = FailureReason.INVALID_REQUEST;
                log.warn("Catalog Ticket not found: ticketId={}", it.ticketId());
                break;
            } catch(CatalogErrorException e) {
                getPriceFailure = FailureReason.CATALOG_ERROR;
                log.warn("Catalog error: ticketId={}", it.ticketId());
                break;
            } catch(CatalogTimeoutException e) {
                getPriceFailure = FailureReason.CATALOG_TIMEOUT;
                log.warn("Catalog timeout: ticketId={}", it.ticketId());
                break;
            } catch(Exception e) {
                getPriceFailure = FailureReason.CATALOG_ERROR;
                log.warn("Catalog Unknow error: ticketId={}", it.ticketId());
                break;
            }
        }

        if(getPriceFailure != null){
            releaseAll(reservedIds, "Get ticket price failed");

            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason(getPriceFailure);
            saidFailedCurrencyAndTotals(order, req, normalized);


            // Add items for audit
            for(CreateOrderRequest.Item it : normalized){
                OrderItem item = new OrderItem();
                item.setTicketId(it.ticketId());
                item.setQty(it.qty());
                item.setUnitPrice(BigDecimal.ZERO);
                order.addItem(item);
            }

            Order savedFailed = orderRepo.save(order);
            log.info("Order FAILED created: userId={}, idemKey={}, orderId={}, reason={}",
                    userId, idemKey, savedFailed.getId(), getPriceFailure);

            markIdemCompleted(userId, idemKey, savedFailed.getId());
            log.info("Idem marked COMPLETED (FAILED): userId={}, idemKey={}, orderId={}", userId, idemKey, savedFailed.getId());

            return new CreateResult(toResponse(savedFailed), true);
        }

        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        pricedItems.forEach(order::addItem);

        Order saved = orderRepo.save(order);
        log.info("Order PENDING created: userId={}, idemKey={}, orderId={}, items={}",
                userId, idemKey, saved.getId(), saved.getOrderItems().size());


        // 7) Commit all reservations (best effort; fail-fast on first error)
        FailureReason commitFailure = null;

        for(String rid : reservedIds) {
            try{
                inventoryClient.commit(rid);
                log.info("Inventory COMMITTED: reservationsId={}", rid);
            } catch (InventoryTimeoutException e) {
                commitFailure = FailureReason.INVENTORY_TIMEOUT;
                log.error("Inventory COMMIT TIMEOUT: reservationId={}", rid, e);
                break;
            } catch (InventoryErrorException e) {
                commitFailure = FailureReason.INVENTORY_ERROR;
                log.error("Inventory COMMIT ERROR: reservationId={}", rid, e);
                break;
            } catch (Exception e) {
                commitFailure = FailureReason.INVENTORY_ERROR;
                log.error("Inventory COMMIT UNKNOWN_ERROR: reservationId={}", rid, e);
                break;
            }
        }

        if (commitFailure != null) {
            // Commit 阶段失败：同步版 MVP 处理 = 标记订单 FAILED（不做复杂补偿）
            saved.setStatus(OrderStatus.FAILED);
            saved.setFailureReason(commitFailure);
            Order savedFailed = orderRepo.save(saved);

            markIdemCompleted(userId, idemKey, savedFailed.getId());
            log.info("Idem marked COMPLETED (FAILED_AFTER_COMMIT): userId={}, idemKey={}, orderId={}, reason={}",
                    userId, idemKey, savedFailed.getId(), commitFailure);

            // 先不做全局异常处理：直接返回 FAILED 订单（你 controller 会给 201）
            return new CreateResult(toResponse(savedFailed), true);
        }

        // 8) All commits ok -> CONFIRMED
        saved.setStatus(OrderStatus.CONFIRMED);
        Order confirmed = orderRepo.save(saved);

        markIdemCompleted(userId, idemKey, confirmed.getId());
        log.info("Idem marked COMPLETED (CONFIRMED): userId={}, idemKey={}, orderId={}",
                userId, idemKey, confirmed.getId());

        // after order saved successfully (orderId exists)
        publisher.publishOrderCreated(
                order.getId(),
                userId,
                idemKey,
                null,
                normalized.stream()
                        .map(it -> new OrderCreatedEvent.Item(it.ticketId(), it.qty()))
                        .toList()
        );

        return new CreateResult(toResponse(confirmed), true);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BadRequestException("orderId must be positive");
        }

        Order order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order Not Found"));

        return toResponse(order);
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

        return orders.map(this::toResponse);
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
            throw new RequestInProgressException();
        }

        Long orderId = idem.getOrderId();

        if (orderId == null) {
            // 理论不应该发生：COMPLETED 但没 orderId
            log.error("Idem anomaly: COMPLETED but orderId is null. userId={}, idemKey={}", userId, idemKey);
            throw new RequestInProgressException();
        }

        Order order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> {
                    log.error("Idem anomaly: COMPLETED but order not found. userId={}, idemKey={}, orderId={}",
                            userId, idemKey, orderId);
                    return new RequestInProgressException();
                });

        return toResponse(order);
    }


    // ------ Helper ------
    private void releaseAll(List<String> reservedIds, String reasonTag) {
        if(reservedIds == null || reservedIds.isEmpty()){
            log.info("ReleaseAll skipped: no reservations. reason={}", reasonTag);
            return;
        }

        for(String rid : reservedIds) {
            try {
                inventoryClient.release(rid);
                log.info("Inventory RELEASED: reservationId={}, reason={}", rid, reasonTag);
            } catch (Exception e) {
                log.error("Inventory RELEASE FAILED: reservationId={}, reason={}, error={}",
                        rid, reasonTag, e.getMessage(), e);
            }
        }
    }

    private List<CreateOrderRequest.Item> normalizeItems(CreateOrderRequest req) {
        if(req.items() == null || req.items().isEmpty()) {
            throw new BadRequestException("items is required");
        }

        // 禁止重复 ticketId（更简单）
        Set<Long> seen = new HashSet<>();
        for (CreateOrderRequest.Item it : req.items()) {
            if(it.ticketId() == null || it.ticketId() <= 0){
                throw new BadRequestException("ticketId must be positive");
            }
            if(it.qty() == null || it.qty() <= 0){
                throw new BadRequestException("qty must be positive");
            }
            if(!seen.add(it.ticketId())){
                throw new BadRequestException("Duplicate ticketId in items: " + it.ticketId());
            }
        }

        return req.items().stream()
                .sorted(Comparator.comparing(CreateOrderRequest.Item::ticketId))
                .toList();
    }

    private void tryInsertIdemInProgress(Long userId, String idemKey) {
        String sql = """
                INSERT INTO idempotency_keys
                    (user_id, idempotency_key, status, order_id)
                VALUES
                    (?, ?, 'IN_PROGRESS', NULL)
                """;
        jdbcTemplate.update(sql, userId, idemKey);
    }

    private void markIdemCompleted(Long userId, String idemKey, Long orderId) {
        String sql = """
                UPDATE idempotency_keys
                SET status = 'COMPLETED',
                    order_id = ?
                WHERE user_id = ? AND idempotency_key = ?
                """;
        jdbcTemplate.update(sql, orderId, userId, idemKey);
    }

    /**
     * Optional helper: keep currency + totals consistent for failed orders.
     * You can also inline this.
     */
    private void saidFailedCurrencyAndTotals(Order failed, CreateOrderRequest req, List<CreateOrderRequest.Item> normalized) {
        failed.setCurrency(req.currency() == null || req.currency().isBlank() ? "USD" : req.currency());
        failed.setTotalAmount(null);
    }

    private OrderResponse toResponse(Order o){
        List<OrderResponse.Item> items = o.getOrderItems().stream()
                .map(i -> new OrderResponse.Item(i.getTicketId(), i.getQty(), i.getUnitPrice()))
                .toList();

        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                o.getStatus().name(),
                o.getFailureReason() == null ? null : o.getFailureReason().name(),
                o.getCurrency(),
                o.getTotalAmount(),
                o.getIdempotencyKey(),
                items,
                o.getCreatedAt()
        );
    }

}
