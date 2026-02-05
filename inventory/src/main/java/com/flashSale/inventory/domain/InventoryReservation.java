package com.flashSale.inventory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Table(
        name = "inventory_reservations",
        indexes = {
            @Index(name = "idx_resv_ticket_id", columnList = "ticket_id"),
            @Index(name = "idx_resv_ticket_status", columnList = "ticket_id, status")
        }
)
public class InventoryReservation {
    @Id
    @Column(name = "reservation_id", columnDefinition = "varchar(64)")
    private String reservationId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(nullable = false)
    private int qty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name="created_at", insertable=false, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", insertable=false, updatable=false)
    private Instant updatedAt;
}
