package com.rental.repository;

import com.rental.entity.RentalTenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalTenantRepository extends JpaRepository<RentalTenant, Long> {

    boolean existsByTenantId(Long tenantId);
}
