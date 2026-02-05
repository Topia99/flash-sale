CREATE TABLE inventory (
    ticket_id BIGINT PRIMARY KEY,
    available INT NOT NULL,
    reserved INT NOT NULL,
    sold INT NOT NULL,
    version BIGINT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (available >= 0),
    CHECK (reserved >= 0),
    CHECK (sold >= 0)
);


CREATE TABLE inventory_reservations (
    reservation_id VARCHAR(64) PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    qty INT NOT NULL,
    status ENUM(
        'INIT',
        'RESERVED',
        'COMMITTED',
        'RELEASED',
        'FAILED'
    ) NOT NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (qty > 0)
);

CREATE INDEX idx_resv_ticket_id
    ON inventory_reservations(ticket_id);

CREATE INDEX idx_resv_ticket_status
    ON inventory_reservations(ticket_id, status);