package com.flashSale.order.domain;

public enum FailureReason {
    INSUFFICIENT_STOCK,
    INVENTORY_TIMEOUT,
    INVENTORY_ERROR,
    INVALID_REQUEST,
    CATALOG_ERROR,
    CATALOG_TIMEOUT
}
