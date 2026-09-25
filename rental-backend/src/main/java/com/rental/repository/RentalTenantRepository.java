package com.rental.repository;

import com.rental.entity.RentalTenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalTenantRepository extends JpaRepository<RentalTenant, Long> {

    boolean existsByTenantId(Long tenantId);

    List<RentalTenant> findByTenantId(Long tenantId);
}
