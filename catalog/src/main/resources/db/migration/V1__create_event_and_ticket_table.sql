CREATE TABLE events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  description TEXT NULL,
  starts_at DATETIME NOT NULL,
  ends_at DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CHECK (ends_at > starts_at)
);

CREATE TABLE tickets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  price NUMERIC(8,2) NOT NULL,
  per_user_limit INT NULL,
  sale_starts_at DATETIME NULL,
  sale_ends_at DATETIME NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OFF_SHELF',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_tickets_event FOREIGN KEY (event_id) REFERENCES events(id),
  CHECK (price > 0),
  CHECK (per_user_limit IS NULL OR per_user_limit >= 1),
  CHECK (
    sale_starts_at IS NULL OR sale_ends_at IS NULL OR sale_ends_at > sale_starts_at
  )
);

CREATE INDEX idx_events_status_starts ON events(status, starts_at);
CREATE INDEX idx_tickets_event_id ON tickets(event_id);
CREATE INDEX idx_tickets_status_starts ON tickets(status, sale_starts_at);