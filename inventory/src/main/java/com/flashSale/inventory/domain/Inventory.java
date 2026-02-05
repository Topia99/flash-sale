package com.flashSale.inventory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Table(name = "inventory")
public class Inventory {
    @Id
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int reserved;

    @Column(nullable = false)
    private int sold;

    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", insertable=false, updatable=false)
    private Instant updatedAt;
}
