package com.rental.service;

import com.rental.dto.*;
import com.rental.entity.*;
import com.rental.entity.enums.*;
import com.rental.mapper.RentalMapper;
import com.rental.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceScheduleAssignmentTest {
    @Mock RentalRepository rentalRepository;
    @Mock RentPaymentRepository rentPaymentRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock TenantRepository tenantRepository;
    @Mock RentalMapper rentalMapper;
    @Mock TenantRentRateService tenantRentRateService;
    @InjectMocks RentalService service;

    private final LocalDate now = YearMonth.now().atDay(1);
    private final LocalDate first = now.minusMonths(2);
    private final LocalDate second = now.minusMonths(1);

    @BeforeEach
    void setUp() {
        when(propertyRepository.findById(5L)).thenReturn(Optional.of(Property.builder()
                .id(5L).monthlyRent(new BigDecimal("7000")).status(PropertyStatus.AVAILABLE).build()));
        when(tenantRepository.findById(8L)).thenReturn(Optional.of(Tenant.builder().id(8L).build()));
        when(rentalRepository.saveAndFlush(any(Rental.class))).thenAnswer(inv -> {
            Rental rental = inv.getArgument(0);
            rental.setId(30L);
            rental.getOccupants().get(0).setId(40L);
            return rental;
        });
    }

    private RentalRequest request(boolean generate) {
        List<RentPeriodRequest> periods = List.of(
                new RentPeriodRequest(first, first, new BigDecimal("3500")),
                new RentPeriodRequest(second, second, new BigDecimal("5000")),
                new RentPeriodRequest(now, null, new BigDecimal("7000")));
        return new RentalRequest(5L, null, null, null, null, null, null, 5,
                RentalStatus.ACTIVE, null,
                List.of(new OccupantRequest(8L, first.plusDays(10), new BigDecimal("7000"), periods, generate)));
    }

    @Test
    void assignmentPersistsScheduleButDoesNotInventHistoricalDebtByDefault() {
        service.create(request(false));
        verify(tenantRentRateService).replacePeriods(argThat(stay -> stay.getId().equals(40L)), anyList());
        verifyNoInteractions(rentPaymentRepository);
    }

    @Test
    void explicitlySelectedBackfillCreatesOnlyUnpaidMonthsAtPeriodSpecificRates() {
        when(rentPaymentRepository.findByRentalIdOrderByPeriodMonthDesc(30L)).thenReturn(List.of());
        when(tenantRentRateService.rentFor(any(RentalTenant.class), any(LocalDate.class))).thenAnswer(inv -> {
            LocalDate month = inv.getArgument(1);
            if (month.equals(first)) return new BigDecimal("3500");
            if (month.equals(second)) return new BigDecimal("5000");
            return new BigDecimal("7000");
        });
        service.create(request(true));
        verify(rentPaymentRepository, times(3)).save(argThat(p ->
                p.getRentalTenant().getId().equals(40L) && p.getStatus() == PaymentStatus.UNPAID
                        && p.getAmountPaid().signum() == 0));
        verify(rentPaymentRepository).save(argThat(p -> p.getPeriodMonth().equals(first)
                && p.getAmountDue().compareTo(new BigDecimal("3500")) == 0));
        verify(rentPaymentRepository).save(argThat(p -> p.getPeriodMonth().equals(second)
                && p.getAmountDue().compareTo(new BigDecimal("5000")) == 0));
        verify(rentPaymentRepository).save(argThat(p -> p.getPeriodMonth().equals(now)
                && p.getAmountDue().compareTo(new BigDecimal("7000")) == 0));
    }
}
