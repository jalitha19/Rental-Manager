package com.rental.repository;

import com.rental.entity.TenantRentRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface TenantRentRateRepository extends JpaRepository<TenantRentRate, Long> {
    List<TenantRentRate> findByRentalTenantIdOrderByEffectiveMonthAsc(Long rentalTenantId);
    Optional<TenantRentRate> findByRentalTenantIdAndEffectiveMonth(Long rentalTenantId, LocalDate effectiveMonth);
}
