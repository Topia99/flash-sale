package com.flashSale.order.client.inventory;

import com.flashSale.order.dto.ReservationResponse;
import com.flashSale.order.dto.ReserveRequest;
import com.flashSale.order.exception.InsufficientStockException;
import com.flashSale.order.exception.InventoryErrorException;
import com.flashSale.order.exception.InventoryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class InventoryClientImpl implements InventoryClient{
    private final RestClient client;

    public InventoryClientImpl() {
        this.client = RestClient.builder()
                .baseUrl("http://localhost:8083")
                .build();
    }


    @Override
    public ReservationResponse reserve(String reservationId, Long ticketId, int qty) {
        ReserveRequest body = new ReserveRequest(reservationId, ticketId, qty);

        try{
            return client.post()
                    .uri("/inventory/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) ->{
                        throw new InsufficientStockException("INSUFFICIENT_STOCK");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new InventoryErrorException("INVENTORY_4XX_" + res.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new InventoryErrorException("INVENTORY_5XX_" + res.getStatusCode().value());
                    })
                    .body(ReservationResponse.class);
        } catch (ResourceAccessException timeoutOrConn) {
            // 连接失败/超时通常在这里（取决于底层 client）
            throw new InventoryTimeoutException("INVENTORY_TIMEOUT", timeoutOrConn);
        } catch (RestClientResponseException e) {
            // 兜底：理论上 onStatus 已处理，但留一层保险
            if (e.getStatusCode().value() == 409) {
                throw new InsufficientStockException("INSUFFICIENT_STOCK");
            }
            throw new InventoryErrorException("INVENTORY_HTTP_" + e.getStatusCode().value());
        }
    }

    @Override
    public ReservationResponse release(String reservationId) {
        try{
            return client.post()
                    .uri("/inventory/reservations/{id}/release", reservationId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        // Inventory: 409 INVALID_STATE (已 COMMITED)
                        throw new InventoryErrorException("INVENTORY_INVALID_STATE_RELEASE");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new InventoryErrorException("INVENTORY_5XX_" + res.getStatusCode().value());
                    })
                    .body(ReservationResponse.class);
        } catch (ResourceAccessException e) {
            throw new InventoryTimeoutException("INVENTORY_TIMEOUT", e);
        }
    }

    @Override
    public ReservationResponse commit(String reservationId) {
        try{
            return client.post()
                    .uri("/inventory/reservations/{id}/commit", reservationId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        // Inventory: 409 INVALID_STATE（已 RELEASED）
                        throw new InventoryErrorException("INVENTORY_INVALID_STATE_COMMIT");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new InventoryErrorException("INVENTORY_5XX_" + res.getStatusCode().value());
                    })
                    .body(ReservationResponse.class);
        } catch (ResourceAccessException e) {
            throw new InventoryTimeoutException("INVENTORY_TIMEOUT", e);
        }
    }
}
