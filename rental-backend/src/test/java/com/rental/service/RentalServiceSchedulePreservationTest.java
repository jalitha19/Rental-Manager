package com.rental.service;

import com.rental.dto.OccupantUpdateRequest;
import com.rental.dto.RentPeriodRequest;
import com.rental.dto.RentalUpdateRequest;
import com.rental.entity.*;
import com.rental.entity.enums.*;
import com.rental.exception.BusinessRuleException;
import com.rental.mapper.RentalMapper;
import com.rental.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceSchedulePreservationTest {
    @Mock RentalRepository rentalRepository;
    @Mock RentPaymentRepository rentPaymentRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock TenantRepository tenantRepository;
    @Mock RentalMapper rentalMapper;
    @Mock TenantRentRateService tenantRentRateService;
    @InjectMocks RentalService service;

    @Test
    void editingHistoricalRatesLeavesOldPaidAndUnpaidInvoicesUntouched() {
        LocalDate now = YearMonth.now().atDay(1);
        LocalDate start = now.minusMonths(2);
        Tenant tenant = Tenant.builder().id(3L).build();
        Rental rental = Rental.builder().id(11L).status(RentalStatus.ACTIVE)
                .property(Property.builder().id(1L).build()).tenant(tenant)
                .startDate(start).monthlyRent(new BigDecimal("3500"))
                .paymentDueDay(5).occupants(new ArrayList<>()).build();
        RentalTenant stay = RentalTenant.builder().id(17L).rental(rental).tenant(tenant)
                .startDate(start).monthlyRent(new BigDecimal("3500")).build();
        rental.getOccupants().add(stay);
        RentPayment unpaid = RentPayment.builder().rental(rental).rentalTenant(stay)
                .periodMonth(start).dueDate(start.plusDays(4)).status(PaymentStatus.UNPAID)
                .amountDue(new BigDecimal("3500")).amountPaid(BigDecimal.ZERO).build();
        RentPayment paid = RentPayment.builder().rental(rental).rentalTenant(stay)
                .periodMonth(start.plusMonths(1)).dueDate(start.plusMonths(1).plusDays(4))
                .status(PaymentStatus.PAID).amountDue(new BigDecimal("3500"))
                .amountPaid(new BigDecimal("3500")).build();
        when(rentalRepository.findById(11L)).thenReturn(Optional.of(rental));
        when(rentPaymentRepository.findByRentalIdOrderByPeriodMonthDesc(11L)).thenReturn(List.of(unpaid, paid));
        when(tenantRentRateService.rentFor(stay, now)).thenReturn(new BigDecimal("7000"));
        List<RentPeriodRequest> periods = List.of(
                new RentPeriodRequest(start, start, new BigDecimal("3500")),
                new RentPeriodRequest(start.plusMonths(1), start.plusMonths(1), new BigDecimal("5000")),
                new RentPeriodRequest(now, null, new BigDecimal("7000")));
        RentalUpdateRequest req = new RentalUpdateRequest(null, null, null, null, 5, null,
                List.of(new OccupantUpdateRequest(17L, start, new BigDecimal("7000"), null, periods, true)));
        service.update(11L, req);
        assertEquals(new BigDecimal("3500"), unpaid.getAmountDue());
        assertEquals(new BigDecimal("3500"), paid.getAmountDue());
        assertEquals(new BigDecimal("7000"), stay.getMonthlyRent());
        verify(tenantRentRateService).replacePeriods(stay, periods);
        verify(rentPaymentRepository, never()).delete(any(RentPayment.class));
        verify(rentPaymentRepository).save(argThat(payment ->
                payment.getPeriodMonth().equals(now) && payment.getStatus() == PaymentStatus.UNPAID
                        && payment.getAmountDue().compareTo(new BigDecimal("7000")) == 0));
    }
}
