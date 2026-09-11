package com.rental.entity;

import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.PropertyType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A house or room that can be rented out. Properties are permanent --
 * occupants change over time via Rental records, but the Property itself
 * is never deleted just because a tenant moves out.
 */
@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short human-friendly code, e.g. "R01", "H01" */
    @Column(name = "property_code", nullable = false, unique = true, length = 30)
    private String propertyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyType type;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(precision = 12, scale = 2)
    private BigDecimal deposit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Rental> rentals = new ArrayList<>();

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
