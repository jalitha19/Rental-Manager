package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One past or current standard/listed rent for a Property, with the date it
 * took effect. A new row is added whenever the property's standard rent
 * changes; existing rows are never edited or removed, so the full history of
 * rate changes is kept. This is separate from what an individual tenant
 * actually pays (see RentalTenant.monthlyRent), which is unaffected by this.
 */
@Entity
@Table(name = "property_rent_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyRentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
