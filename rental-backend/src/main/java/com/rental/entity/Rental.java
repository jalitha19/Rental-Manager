package com.rental.entity;

import com.rental.entity.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The core "occupancy" record: connects a Tenant to a Property for a specific
 * period. A Property can have many Rentals over time (history), but only one
 * with status = ACTIVE at any given moment.
 *
 * Rent/deposit are snapshotted here at the time the rental was created, so
 * changing a Property's current rent later does not rewrite history.
 */
@Entity
@Table(name = "rentals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * Optional second tenant for a property that can accommodate 2 occupants.
     * If present, this rental accommodates 2 tenants.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant2_id")
    private Tenant tenant2;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Null while the rental is ACTIVE
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(name = "payment_due_day")
    private Integer paymentDueDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RentalStatus status = RentalStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "rental", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<RentPayment> payments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
