package com.rental.service;

import com.rental.entity.RentalTenant;
import com.rental.entity.TenantRentRate;
import com.rental.repository.TenantRentRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class TenantRentRateService {
    private final TenantRentRateRepository repository;

    /** Use the rate applicable to the invoice month, not today's rate. */
    public BigDecimal rentFor(RentalTenant occupant, LocalDate month) {
        List<TenantRentRate> rates = repository.findByRentalTenantIdOrderByEffectiveMonthAsc(occupant.getId());
        BigDecimal rate = rates.isEmpty() ? occupant.getMonthlyRent() : rates.get(0).getMonthlyRent();
        for (TenantRentRate entry : rates) {
            if (entry.getEffectiveMonth().isAfter(month)) break;
            rate = entry.getMonthlyRent();
        }
        return rate;
    }

    /** Initialize a historical baseline on the first change; then append rate transitions. */
    public void recordChange(RentalTenant occupant, BigDecimal oldRent, BigDecimal newRent, LocalDate effectiveMonth) {
        if (repository.findByRentalTenantIdOrderByEffectiveMonthAsc(occupant.getId()).isEmpty()) {
            repository.save(TenantRentRate.builder().rentalTenant(occupant)
                    .effectiveMonth(occupant.getStartDate().withDayOfMonth(1))
                    .monthlyRent(oldRent).build());
        }
        // If first change is in the tenant's original start month, replace baseline.
        TenantRentRate rate = repository.findByRentalTenantIdAndEffectiveMonth(occupant.getId(), effectiveMonth)
                .orElseGet(() -> TenantRentRate.builder().rentalTenant(occupant)
                        .effectiveMonth(effectiveMonth).build());
        rate.setMonthlyRent(newRent);
        repository.save(rate);
    }
}
