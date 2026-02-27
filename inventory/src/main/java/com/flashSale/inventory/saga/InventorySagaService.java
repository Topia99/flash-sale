package com.flashSale.inventory.saga;

import com.flashSale.inventory.events.OrderCreatedEvent;
import com.flashSale.inventory.exception.ConflictException;
import com.flashSale.inventory.exception.NotFoundException;
import com.flashSale.inventory.exception.OutOfStockException;
import com.flashSale.inventory.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySagaService {

    private final InventoryReservationService reservationService;

    /**
     * 处理整单：
     * 1) 逐 item reserve
     * 2) 全 reserve 成功 -> 逐 item commit
     * 3) 任一失败 -> release 已 reserve 的补偿
     */
    public InventorySagaResult handleOrderCreated(OrderCreatedEvent event) {
        if(event == null || event.getEnvelope() == null || event.getItems() == null || event.getItems().isEmpty()) {
            return InventorySagaResult.failed(InventoryFailReason.INVALID_REQUEST);
        }

        String idemKey = event.getEnvelope().getIdempotencyKey();
        if(idemKey == null || idemKey.isBlank()) {
            return InventorySagaResult.failed(InventoryFailReason.INVALID_REQUEST);
        }

        // 已成功 reserve 的 reservationIds（用于失败补偿 release）
        List<String> reserved = new ArrayList<>();

        try {
            // A) Reserve phase
            for(OrderCreatedEvent.Item it : event.getItems()) {
                if(it == null || it.ticketId() == null || it.qty() == null) {
                    throw new IllegalArgumentException("Invalid item");
                }

                String reservationId = reservationId(idemKey, it.ticketId());

                // reserve 应该是幂等：重复 reserve 也应该返回成功或 no-op
                reservationService.reserve(reservationId, it.ticketId(), it.qty());
                reserved.add(reservationId);
            }

            // B) Commit phase （全部 reserve 成功才会到这里）
            for (String reservationId : reserved) {
                reservationService.commit(reservationId);
            }

            return InventorySagaResult.committed();
        } catch (NotFoundException e) {
            // 请求数据不合法
            log.warn("Ticket Id Not Found. orderId={}, idemKey={}, msg={}",
                    event.getEnvelope().getOrderId(), idemKey, e.getMessage());
            safeReleaseAll(reserved, "Ticket Not Found");
            return InventorySagaResult.failed(InventoryFailReason.INVALID_REQUEST);
        } catch (OutOfStockException e) {
            // 业务失败：库存不足
            log.warn("Out of stock during reserve. orderId={}, idemKey={}, msg={}",
                    event.getEnvelope().getOrderId(), idemKey, e.getMessage());
            safeReleaseAll(reserved, "OUT_OF_STOCK");
            return InventorySagaResult.failed(InventoryFailReason.OUT_OF_STOCK);
        } catch (Exception e) {
            // 系统错误：DB 超时/连接问题等
            log.error("Internal error handling OrderCreated. orderId={}, idemKey={}",
                    event.getEnvelope().getOrderId(), idemKey, e);
            safeReleaseAll(reserved, "INTERNAL_ERROR");
            return InventorySagaResult.failed(InventoryFailReason.INTERNAL_ERROR);
        }
    }

    private void safeReleaseAll(List<String> reservedIds, String reason) {
        for(String rid : reservedIds) {
            try {
                reservationService.release(rid);
            } catch (Exception ex) {
                // release 失败不要影响主流程返回，但必须记日志（后续可做对账任务）
                log.error("Release failed. reservationId={}, reason={}", rid, reason, ex);
            }
        }
    }



    private String reservationId(String idemKey, Long ticketId) {
        return idemKey + ":" + ticketId;
    }
}
