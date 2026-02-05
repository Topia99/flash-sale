package com.flashSale.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReserveRequest(
        @NotBlank
        @Size(max=64)
        String reservationId,

        @NotNull
        Long ticketId,

        @Min(1)
        int qty
) {
}
