package com.rental.entity;

import com.rental.entity.enums.TenantType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A tenant -- either an individual person or a company. Editing a tenant's
 * personal information must never affect their rental/payment history, since
 * history hangs off Rental/RentPayment records, not this entity.
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", nullable = false, length = 20)
    @Builder.Default
    private TenantType tenantType = TenantType.INDIVIDUAL;

    /**
     * Person's full name, or company name if tenantType == COMPANY
     */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * NIC/ID number -- only applies to INDIVIDUAL tenants
     */
    @Column(name = "nic_number", length = 50)
    private String nicNumber;

    /**
     * Only applies to COMPANY tenants
     */
    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column
    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "photo_url", length = 1000)
    private String photoUrl;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TenantPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = false)
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
