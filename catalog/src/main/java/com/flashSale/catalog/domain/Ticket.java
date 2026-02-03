package com.flashSale.catalog.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tickets",
        indexes = {
                @Index(name = "idx_tickets_event_id", columnList = "event_id"),
                @Index(name = "idx_tickets_status_sale_starts", columnList = "status, sale_starts_at")
        })
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="event_id", nullable=false)
    private Event event;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, precision=8, scale=2)
    private BigDecimal price;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "sale_starts_at")
    private Instant saleStartsAt;

    @Column(name = "sale_ends_at")
    private Instant saleEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // 输入默认值
    @PrePersist
    void prePersist() {
        if (status == null) status = TicketStatus.OFF_SHELF;
    }
}
