package com.rental.repository;

import com.rental.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Matches by name, NIC number, or any of the tenant's phone numbers --
     * case-insensitive. Covers Section 5's requirement that searching a
     * phone number or a partial name both find the right tenant.
     */
    @Query("""
            SELECT DISTINCT t FROM Tenant t
            LEFT JOIN t.phones p
            WHERE LOWER(t.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(t.nicNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR p.phoneNumber LIKE CONCAT('%', :q, '%')
            ORDER BY t.fullName
            """)
    List<Tenant> search(@Param("q") String query);
}
