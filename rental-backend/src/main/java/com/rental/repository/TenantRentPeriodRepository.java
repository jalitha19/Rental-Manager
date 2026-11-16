package com.rental.repository;

import com.rental.entity.TenantRentPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TenantRentPeriodRepository extends JpaRepository<TenantRentPeriod, Long> {
    List<TenantRentPeriod> findByRentalTenantIdOrderByStartDateAsc(Long rentalTenantId);
}
