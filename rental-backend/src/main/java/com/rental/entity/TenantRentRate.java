package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Effective-month ledger for an individual occupant's rent. */
@Entity
@Table(name = "tenant_rent_rates", uniqueConstraints = @UniqueConstraint(columnNames = {"rental_tenant_id", "effective_month"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantRentRate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rental_tenant_id", nullable = false)
    private RentalTenant rentalTenant;
    @Column(name = "effective_month", nullable = false)
    private LocalDate effectiveMonth;
    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;
}
