package com.flashSale.inventory.controller;

import com.flashSale.inventory.dto.ReservationResponse;
import com.flashSale.inventory.dto.ReserveRequest;
import com.flashSale.inventory.service.InventoryReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryReservationService reservationService;

    @PostMapping("/reservations")
    public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
        return reservationService.reserve(
                request.reservationId(),
                request.ticketId(),
                request.qty()
        );
    }
}
