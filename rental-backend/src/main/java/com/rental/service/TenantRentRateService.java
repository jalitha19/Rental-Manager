package com.rental.service;

import com.rental.dto.RentPeriodRequest;
import com.rental.dto.RentPeriodResponse;
import com.rental.entity.RentalTenant;
import com.rental.entity.TenantRentPeriod;
import com.rental.entity.TenantRentRate;
import com.rental.exception.BusinessRuleException;
import com.rental.repository.TenantRentPeriodRepository;
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
    private final TenantRentPeriodRepository datedRepository;

    /**
     * One monthly invoice uses the rate covering the earliest billable date in
     * that month. No automatic proration. Months entirely in a date gap have
     * no defined rate and must NOT generate an invoice.
     */
    public BigDecimal rentFor(RentalTenant occupant, LocalDate month) {
        List<TenantRentPeriod> dated = datedRepository.findByRentalTenantIdOrderByStartDateAsc(occupant.getId());
        if (!dated.isEmpty()) {
            LocalDate first = month.withDayOfMonth(1);
            LocalDate last = YearMonth.from(month).atEndOfMonth();
            if (occupant.getStartDate() != null && occupant.getStartDate().isAfter(first)) first = occupant.getStartDate();
            if (occupant.getEndDate() != null && occupant.getEndDate().isBefore(last)) last = occupant.getEndDate();
            if (first.isAfter(last)) return null;
            for (TenantRentPeriod period : dated) {
                LocalDate from = period.getStartDate().isAfter(first) ? period.getStartDate() : first;
                LocalDate until = period.getEndDate() != null && period.getEndDate().isBefore(last) ? period.getEndDate() : last;
                if (!from.isAfter(until)) return period.getMonthlyRent();
            }
            return null;
        }
        // Legacy V7 rates are preserved. No historical payment rows are touched.
        List<TenantRentRate> rates = repository.findByRentalTenantIdOrderByEffectiveMonthAsc(occupant.getId());
        BigDecimal rate = rates.isEmpty() ? occupant.getMonthlyRent() : rates.get(0).getMonthlyRent();
        for (TenantRentRate entry : rates) {
            if (entry.getEffectiveMonth().isAfter(month)) break;
            rate = entry.getMonthlyRent();
        }
        return rate;
    }

    public List<RentPeriodResponse> periodsFor(Long occupantId) {
        List<TenantRentPeriod> dated = datedRepository.findByRentalTenantIdOrderByStartDateAsc(occupantId);
        if (!dated.isEmpty()) {
            return dated.stream().map(p -> new RentPeriodResponse(
                    p.getStartDate(), p.getEndDate(), p.getMonthlyRent())).toList();
        }
        // V7 stores rate transitions, not explicit interval boundaries.
        List<TenantRentRate> legacy = repository.findByRentalTenantIdOrderByEffectiveMonthAsc(occupantId);
        List<RentPeriodResponse> periods = new ArrayList<>();
        for (int i = 0; i < legacy.size(); i++) {
            LocalDate end = i + 1 < legacy.size() ? legacy.get(i + 1).getEffectiveMonth().minusDays(1) : null;
            periods.add(new RentPeriodResponse(legacy.get(i).getEffectiveMonth(), end, legacy.get(i).getMonthlyRent()));
        }
        return periods;
    }

    /** Arbitrary days and gaps are accepted; inclusive overlaps are rejected. */
    public void validatePeriods(LocalDate tenantStartDate, List<RentPeriodRequest> periods) {
        if (tenantStartDate == null) throw new BusinessRuleException("Tenant start date is required");
        if (periods == null || periods.isEmpty() || periods.size() > 100) {
            throw new BusinessRuleException("Provide between 1 and 100 rent periods");
        }
        LocalDate previousEnd = null;
        for (int i = 0; i < periods.size(); i++) {
            RentPeriodRequest p = periods.get(i);
            if (p == null || p.startDate() == null || p.monthlyRent() == null || p.monthlyRent().signum() < 0) {
                throw new BusinessRuleException("Every rent period needs a start date and nonnegative monthly rent");
            }
            if (p.endDate() != null && p.endDate().isBefore(p.startDate())) {
                throw new BusinessRuleException("Rent period end date cannot precede its start date");
            }
            if (previousEnd != null && !p.startDate().isAfter(previousEnd)) {
                throw new BusinessRuleException("Rent periods cannot overlap; gaps are allowed");
            }
            if (i < periods.size() - 1) {
                if (p.endDate() == null) throw new BusinessRuleException("Historical rent periods need an end date");
            } else if (p.endDate() != null) {
                throw new BusinessRuleException("The current rent period must be ongoing");
            }
            previousEnd = p.endDate();
        }
        // The top entry represents rent that has already become current.
        if (periods.get(periods.size() - 1).startDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Current rent cannot begin in the future");
        }
    }

    /** Updates only the price-period ledger; rent_payments are never deleted/rewritten. */
    @Transactional
    public void replacePeriods(RentalTenant occupant, List<RentPeriodRequest> periods) {
        validatePeriods(occupant.getStartDate(), periods);
        List<TenantRentPeriod> existing = datedRepository.findByRentalTenantIdOrderByStartDateAsc(occupant.getId());
        Set<LocalDate> requestedStarts = new HashSet<>();
        for (RentPeriodRequest p : periods) {
            requestedStarts.add(p.startDate());
            TenantRentPeriod rate = existing.stream().filter(r -> r.getStartDate().equals(p.startDate()))
                    .findFirst().orElseGet(() -> TenantRentPeriod.builder()
                            .rentalTenant(occupant).startDate(p.startDate()).build());
            rate.setEndDate(p.endDate());
            rate.setMonthlyRent(p.monthlyRent());
            datedRepository.save(rate);
        }
        for (TenantRentPeriod old : existing) {
            if (!requestedStarts.contains(old.getStartDate())) datedRepository.delete(old);
        }
    }

    /** Older current-month-only workflow; updates the dated schedule if one exists. */
    @Transactional
    public void recordChange(RentalTenant occupant, BigDecimal oldRent, BigDecimal newRent, LocalDate effectiveMonth) {
        List<TenantRentPeriod> dated = datedRepository.findByRentalTenantIdOrderByStartDateAsc(occupant.getId());
        if (!dated.isEmpty()) {
            TenantRentPeriod latest = dated.get(dated.size() - 1);
            if (latest.getStartDate().isAfter(effectiveMonth)) {
                throw new BusinessRuleException("Current rent change conflicts with a later price period");
            }
            if (latest.getStartDate().equals(effectiveMonth)) {
                latest.setMonthlyRent(newRent);
                datedRepository.save(latest);
            } else {
                // Do not fill an intentional gap between this rate and the change.
                if (latest.getEndDate() == null || !latest.getEndDate().isBefore(effectiveMonth)) {
                    latest.setEndDate(effectiveMonth.minusDays(1));
                    datedRepository.save(latest);
                }
                datedRepository.save(TenantRentPeriod.builder().rentalTenant(occupant)
                        .startDate(effectiveMonth).monthlyRent(newRent).build());
            }
            return;
        }
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
