package com.flashSale.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InitStockRequest {
    @Min(value = 0, message = "available must be >= 0")
    private int available;
}
