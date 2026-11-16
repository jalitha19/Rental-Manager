package com.rental.service;

import com.rental.dto.RentPeriodRequest;
import com.rental.dto.RentPeriodResponse;
import com.rental.entity.RentalTenant;
import com.rental.entity.TenantRentPeriod;
import com.rental.entity.TenantRentRate;
import com.rental.exception.BusinessRuleException;
import com.rental.repository.TenantRentPeriodRepository;
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
    @Mock TenantRentPeriodRepository datedRepository;
    @InjectMocks TenantRentRateService service;

    private RentPeriodRequest period(String start, String end, String rent) {
        return new RentPeriodRequest(LocalDate.parse(start), end == null ? null : LocalDate.parse(end), new BigDecimal(rent));
    }

    @Test
    void acceptsGapsAndArbitraryDatesButRejectsOverlaps() {
        List<RentPeriodRequest> periods = List.of(
                period("2018-01-12", "2022-07-18", "3500"),
                period("2022-08-09", "2025-10-21", "5000"),
                period("2025-11-17", null, "7000"));
        assertDoesNotThrow(() -> service.validatePeriods(LocalDate.of(2018, 1, 12), periods));
        assertThrows(BusinessRuleException.class, () -> service.validatePeriods(LocalDate.of(2018, 1, 12), List.of(
                period("2018-01-12", "2022-07-18", "3500"),
                period("2022-07-18", null, "5000"))));
        assertThrows(BusinessRuleException.class, () -> service.validatePeriods(LocalDate.of(2018, 1, 12), List.of(
                period("2018-01-12", null, "3500"),
                period("2022-08-09", null, "5000"))));
    }

    @Test
    void rateForFirstCoveredDayAndSkipsAnEntireGapMonth() {
        RentalTenant occupant = RentalTenant.builder().id(19L).startDate(LocalDate.of(2018, 1, 12))
                .monthlyRent(new BigDecimal("7000")).build();
        when(datedRepository.findByRentalTenantIdOrderByStartDateAsc(19L)).thenReturn(List.of(
                TenantRentPeriod.builder().rentalTenant(occupant).startDate(LocalDate.of(2018, 1, 12))
                        .endDate(LocalDate.of(2022, 7, 18)).monthlyRent(new BigDecimal("3500")).build(),
                TenantRentPeriod.builder().rentalTenant(occupant).startDate(LocalDate.of(2022, 9, 9))
                        .endDate(LocalDate.of(2025, 10, 21)).monthlyRent(new BigDecimal("5000")).build(),
                TenantRentPeriod.builder().rentalTenant(occupant).startDate(LocalDate.of(2025, 11, 17))
                        .monthlyRent(new BigDecimal("7000")).build()));
        assertEquals(new BigDecimal("3500"), service.rentFor(occupant, LocalDate.of(2022, 7, 1)));
        assertNull(service.rentFor(occupant, LocalDate.of(2022, 8, 1)));
        assertEquals(new BigDecimal("5000"), service.rentFor(occupant, LocalDate.of(2022, 9, 1)));
        assertEquals(new BigDecimal("7000"), service.rentFor(occupant, LocalDate.of(2025, 11, 1)));
        assertEquals(LocalDate.of(2022, 7, 18), service.periodsFor(19L).get(0).endDate());
    }

    @Test
    void legacyV7RatesStillWorkAndEndAtMonthEnd() {
        RentalTenant occupant = RentalTenant.builder().id(19L).startDate(LocalDate.of(2018, 1, 12))
                .monthlyRent(new BigDecimal("5000")).build();
        when(repository.findByRentalTenantIdOrderByEffectiveMonthAsc(19L)).thenReturn(List.of(
                TenantRentRate.builder().effectiveMonth(LocalDate.of(2018, 1, 1))
                        .monthlyRent(new BigDecimal("3500")).build(),
                TenantRentRate.builder().effectiveMonth(LocalDate.of(2022, 8, 1))
                        .monthlyRent(new BigDecimal("5000")).build()));
        assertEquals(new BigDecimal("3500"), service.rentFor(occupant, LocalDate.of(2019, 3, 1)));
        assertEquals(new BigDecimal("5000"), service.rentFor(occupant, LocalDate.of(2022, 8, 1)));
        List<RentPeriodResponse> results = service.periodsFor(19L);
        assertEquals(LocalDate.of(2022, 7, 31), results.get(0).endDate());
        assertEquals(LocalDate.of(2022, 7, 1), results.get(0).endMonth());
    }

    @Test
    void replacingDateTimelineOnlyUsesPricePeriodRepository() {
        RentalTenant occupant = RentalTenant.builder().id(19L).startDate(LocalDate.of(2018, 1, 12)).build();
        when(datedRepository.findByRentalTenantIdOrderByStartDateAsc(19L)).thenReturn(List.of());
        service.replacePeriods(occupant, List.of(
                period("2018-01-12", "2022-07-18", "3500"),
                period("2022-08-09", "2025-10-21", "5000"),
                period("2025-11-17", null, "7000")));
        verify(datedRepository, times(3)).save(any(TenantRentPeriod.class));
        verifyNoInteractions(repository);
    }
}
