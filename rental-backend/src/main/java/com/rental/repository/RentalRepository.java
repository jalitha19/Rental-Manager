package com.rental.repository;

import com.rental.entity.Rental;
import com.rental.entity.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByPropertyIdOrderByStartDateDesc(Long propertyId);

    List<Rental> findByTenantIdOrderByStartDateDesc(Long tenantId);

    Optional<Rental> findByPropertyIdAndStatus(Long propertyId, RentalStatus status);

    List<Rental> findByStatus(RentalStatus status);

    /**
     * Powers the main Rentals page filters -- any combination of property,
     * tenant, and status, all optional.
     */
    @Query("""
            SELECT r FROM Rental r
            WHERE (:propertyId IS NULL OR r.property.id = :propertyId)
              AND (:tenantId IS NULL OR r.tenant.id = :tenantId)
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.startDate DESC
            """)
    List<Rental> filter(
            @Param("propertyId") Long propertyId,
            @Param("tenantId") Long tenantId,
            @Param("status") RentalStatus status
    );
}
