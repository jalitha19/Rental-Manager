package com.rental.service;

import com.rental.dto.RentPeriodRequest;
import com.rental.dto.RentPeriodResponse;
import com.rental.entity.RentalTenant;
import com.rental.entity.TenantRentRate;
import com.rental.exception.BusinessRuleException;
import com.rental.repository.TenantRentRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TenantRentRateService {
    private final TenantRentRateRepository repository;

    public BigDecimal rentFor(RentalTenant occupant, LocalDate month) {
        List<TenantRentRate> rates = repository.findByRentalTenantIdOrderByEffectiveMonthAsc(occupant.getId());
        BigDecimal rate = rates.isEmpty() ? occupant.getMonthlyRent() : rates.get(0).getMonthlyRent();
        for (TenantRentRate entry : rates) {
            if (entry.getEffectiveMonth().isAfter(month)) break;
            rate = entry.getMonthlyRent();
        }
        return rate;
    }

    /** Rate transitions are the source of truth; end dates are derived from the next transition. */
    public List<RentPeriodResponse> periodsFor(Long occupantId) {
        List<TenantRentRate> rates = repository.findByRentalTenantIdOrderByEffectiveMonthAsc(occupantId);
        List<RentPeriodResponse> periods = new ArrayList<>();
        for (int i = 0; i < rates.size(); i++) {
            LocalDate end = i + 1 < rates.size() ? rates.get(i + 1).getEffectiveMonth().minusMonths(1) : null;
            periods.add(new RentPeriodResponse(rates.get(i).getEffectiveMonth(), end, rates.get(i).getMonthlyRent()));
        }
        return periods;
    }

    /** Strict validation prevents overlapping, missing, or incorrectly ordered monthly prices. */
    public void validatePeriods(LocalDate tenantStartDate, List<RentPeriodRequest> periods) {
        if (tenantStartDate == null) throw new BusinessRuleException("Tenant start date is required");
        if (periods == null || periods.isEmpty() || periods.size() > 100) {
            throw new BusinessRuleException("Provide between 1 and 100 rent periods");
        }
        LocalDate expectedStart = tenantStartDate.withDayOfMonth(1);
        for (int i = 0; i < periods.size(); i++) {
            RentPeriodRequest p = periods.get(i);
            if (p == null || p.startMonth() == null || p.monthlyRent() == null || p.monthlyRent().signum() < 0) {
                throw new BusinessRuleException("Every rent period needs a start month and nonnegative monthly rent");
            }
            if (p.startMonth().getDayOfMonth() != 1 || (p.endMonth() != null && p.endMonth().getDayOfMonth() != 1)) {
                throw new BusinessRuleException("Rent periods must use the first day of each month");
            }
            if (p.startMonth().isAfter(YearMonth.now().atDay(1))) {
                throw new BusinessRuleException("Future rent periods are not supported yet; rates must not become active early");
            }
            if (!p.startMonth().equals(expectedStart)) {
                throw new BusinessRuleException("Rent periods must be chronological, continuous, and begin in the tenant's start month (expected " + expectedStart + ")");
            }
            if (i < periods.size() - 1) {
                if (p.endMonth() == null || p.endMonth().isBefore(p.startMonth())) {
                    throw new BusinessRuleException("Each historical rent period needs an end month on or after its start");
                }
                expectedStart = p.endMonth().plusMonths(1);
            } else if (p.endMonth() != null) {
                throw new BusinessRuleException("The latest rent period must have no end month (ongoing)");
            }
        }
    }

    /**
     * Replace only the effective-rate ledger for one occupant. Financial transactions
     * in rent_payments are NEVER updated or deleted by this operation.
     */
    @Transactional
    public void replacePeriods(RentalTenant occupant, List<RentPeriodRequest> periods) {
        validatePeriods(occupant.getStartDate(), periods);
        List<TenantRentRate> existing = repository.findByRentalTenantIdOrderByEffectiveMonthAsc(occupant.getId());
        Set<LocalDate> requestedMonths = new HashSet<>();
        for (RentPeriodRequest p : periods) {
            requestedMonths.add(p.startMonth());
            TenantRentRate rate = existing.stream()
                    .filter(r -> r.getEffectiveMonth().equals(p.startMonth()))
                    .findFirst()
                    .orElseGet(() -> TenantRentRate.builder().rentalTenant(occupant)
                            .effectiveMonth(p.startMonth()).build());
            rate.setMonthlyRent(p.monthlyRent());
            repository.save(rate);
        }
        // Deleting unused transitions only changes the rate ledger, not invoices.
        for (TenantRentRate previous : existing) {
            if (!requestedMonths.contains(previous.getEffectiveMonth())) repository.delete(previous);
        }
    }

    /** Existing current-month-only workflow used by older API clients. */
    @Transactional
    public void recordChange(RentalTenant occupant, BigDecimal oldRent, BigDecimal newRent, LocalDate effectiveMonth) {
        if (repository.findByRentalTenantIdOrderByEffectiveMonthAsc(occupant.getId()).isEmpty()) {
            repository.save(TenantRentRate.builder().rentalTenant(occupant)
                    .effectiveMonth(occupant.getStartDate().withDayOfMonth(1)).monthlyRent(oldRent).build());
        }
        TenantRentRate rate = repository.findByRentalTenantIdAndEffectiveMonth(occupant.getId(), effectiveMonth)
                .orElseGet(() -> TenantRentRate.builder().rentalTenant(occupant)
                        .effectiveMonth(effectiveMonth).build());
        rate.setMonthlyRent(newRent);
        repository.save(rate);
    }
}
