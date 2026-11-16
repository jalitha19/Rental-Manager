package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** An explicitly dated rent interval; gaps between records are meaningful. */
@Entity
@Table(name = "tenant_rent_periods", uniqueConstraints = @UniqueConstraint(columnNames = {"rental_tenant_id", "start_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantRentPeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rental_tenant_id", nullable = false)
    private RentalTenant rentalTenant;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;
}
