CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,

    status ENUM('PENDING','CONFIRMED','FAILED') NOT NULL DEFAULT 'PENDING',

    total_amount DECIMAL(10, 2) NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',

    idempotency_key VARCHAR(128) NOT NULL,
    failure_reason ENUM('INSUFFICIENT_STOCK','INVENTORY_TIMEOUT','INVENTORY_ERROR','INVALID_REQUEST') NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_orders_user_idem (user_id, idempotency_key),
    KEY idx_orders_user_created_at (user_id, created_at) -- INDEX
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,

    ticket_id BIGINT NOT NULL,
    qty INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CHECK(qty > 0),

    KEY idx_order_items_order_id (order_id),
    KEY idx_order_items_ticket_id (ticket_id),

    CONSTRAINT fk_order_items_order
            FOREIGN KEY (order_id) REFERENCES orders(id)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE idempotency_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,

    status ENUM('IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS',

    order_id BIGINT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_idem_user_key (user_id, idempotency_key),
    KEY idx_idem_order_id (order_id),

    CONSTRAINT fk_idem_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
