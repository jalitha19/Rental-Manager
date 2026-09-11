package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A single phone number belonging to a tenant. Kept as its own table
 * (rather than a comma-separated column) so tenants can have any number
 * of phone numbers and each can be added/edited/removed independently.
 */
@Entity
@Table(name = "tenant_phones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    /** Optional free-text label, e.g. "mobile", "work", "whatsapp" */
    @Column(length = 30)
    private String label;
}
