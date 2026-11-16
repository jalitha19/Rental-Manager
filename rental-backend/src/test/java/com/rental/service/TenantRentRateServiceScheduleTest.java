package com.rental.service;

import com.rental.dto.RentPeriodRequest;
import com.rental.dto.RentPeriodResponse;
import com.rental.entity.RentalTenant;
import com.rental.entity.TenantRentRate;
import com.rental.exception.BusinessRuleException;
import com.rental.repository.TenantRentRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantRentRateServiceScheduleTest {
    @Mock TenantRentRateRepository repository;
    @InjectMocks TenantRentRateService service;

    private RentPeriodRequest period(String start, String end, String rent) {
        return new RentPeriodRequest(LocalDate.parse(start), end == null ? null : LocalDate.parse(end), new BigDecimal(rent));
    }

    @Test
    void validatesClientThreePriceExampleAndSelectsCorrectRateForEachMonth() {
        List<RentPeriodRequest> periods = List.of(
                period("2018-01-01", "2022-07-01", "3500"),
                period("2022-08-01", "2025-10-01", "5000"),
                period("2025-11-01", null, "7000"));
        service.validatePeriods(LocalDate.of(2018, 1, 17), periods);
        RentalTenant occupant = RentalTenant.builder().id(19L).monthlyRent(new BigDecimal("7000")).build();
        when(repository.findByRentalTenantIdOrderByEffectiveMonthAsc(19L)).thenReturn(List.of(
                TenantRentRate.builder().rentalTenant(occupant).effectiveMonth(periods.get(0).startMonth()).monthlyRent(periods.get(0).monthlyRent()).build(),
                TenantRentRate.builder().rentalTenant(occupant).effectiveMonth(periods.get(1).startMonth()).monthlyRent(periods.get(1).monthlyRent()).build(),
                TenantRentRate.builder().rentalTenant(occupant).effectiveMonth(periods.get(2).startMonth()).monthlyRent(periods.get(2).monthlyRent()).build()));
        assertEquals(new BigDecimal("3500"), service.rentFor(occupant, LocalDate.of(2022, 7, 1)));
        assertEquals(new BigDecimal("5000"), service.rentFor(occupant, LocalDate.of(2022, 8, 1)));
        assertEquals(new BigDecimal("5000"), service.rentFor(occupant, LocalDate.of(2025, 10, 1)));
        assertEquals(new BigDecimal("7000"), service.rentFor(occupant, LocalDate.of(2025, 11, 1)));
        List<RentPeriodResponse> result = service.periodsFor(19L);
        assertEquals(LocalDate.of(2022, 7, 1), result.get(0).endMonth());
        assertEquals(LocalDate.of(2025, 10, 1), result.get(1).endMonth());
        assertNull(result.get(2).endMonth());
    }

    @Test
    void rejectsGapsAndOverlappingIntervals() {
        assertThrows(BusinessRuleException.class, () -> service.validatePeriods(LocalDate.of(2018, 1, 15), List.of(
                period("2018-01-01", "2022-07-01", "3500"),
                period("2022-09-01", null, "5000"))));
        assertThrows(BusinessRuleException.class, () -> service.validatePeriods(LocalDate.of(2018, 1, 15), List.of(
                period("2018-01-01", "2022-07-01", "3500"),
                period("2022-07-01", null, "5000"))));
        assertThrows(BusinessRuleException.class, () -> service.validatePeriods(LocalDate.of(2018, 1, 15), List.of(
                period("2018-01-01", null, "3500"),
                period("2022-08-01", null, "5000"))));
    }

    @Test
    void replacingRateLedgerDoesNotUsePaymentRepository() {
        RentalTenant occupant = RentalTenant.builder().id(19L).startDate(LocalDate.of(2018, 1, 17)).build();
        when(repository.findByRentalTenantIdOrderByEffectiveMonthAsc(19L)).thenReturn(List.of());
        service.replacePeriods(occupant, List.of(
                period("2018-01-01", "2022-07-01", "3500"),
                period("2022-08-01", "2025-10-01", "5000"),
                period("2025-11-01", null, "7000")));
        verify(repository, times(3)).save(any(TenantRentRate.class));
        verify(repository, never()).delete(any(TenantRentRate.class));
    }
}
