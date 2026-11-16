package com.rental.service;

import com.rental.dto.OccupantUpdateRequest;
import com.rental.dto.RentalUpdateRequest;
import com.rental.entity.*;
import com.rental.entity.enums.*;
import com.rental.mapper.RentalMapper;
import com.rental.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTermsPaymentTest {
    @Mock RentalRepository rentalRepository;
    @Mock RentPaymentRepository rentPaymentRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock TenantRepository tenantRepository;
    @Mock RentalMapper rentalMapper;
    @InjectMocks RentalService service;

    @Mock TenantRentRateService tenantRentRateService;

    @Test
    void rentChangePreservesPastUnpaidAndPaidButUpdatesCurrentUntouched() {
        Rental rental = rental();
        RentalTenant stay = rental.getOccupants().get(0);
        LocalDate priorMonth = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        RentPayment historicUnpaid = invoice(rental, stay, priorMonth, PaymentStatus.UNPAID);
        RentPayment historicPaid = invoice(rental, stay, priorMonth.minusMonths(1), PaymentStatus.PAID);
        historicPaid.setAmountPaid(new BigDecimal("12000"));
        RentPayment currentUnpaid = invoice(rental, stay, currentMonth, PaymentStatus.UNPAID);
        when(rentalRepository.findById(12L)).thenReturn(Optional.of(rental));
        when(rentPaymentRepository.findByRentalIdOrderByPeriodMonthDesc(12L))
                .thenReturn(List.of(historicUnpaid, historicPaid, currentUnpaid));
        when(tenantRentRateService.rentFor(stay, currentMonth)).thenReturn(new BigDecimal("15000"));
        service.update(12L, request(LocalDate.of(2026, 1, 10), "15000"));
        assertEquals(new BigDecimal("12000"), historicUnpaid.getAmountDue());
        assertEquals(new BigDecimal("12000"), historicPaid.getAmountDue());
        assertEquals(new BigDecimal("15000"), currentUnpaid.getAmountDue());
        verify(rentPaymentRepository, never()).delete(any(RentPayment.class));
        verify(tenantRentRateService).recordChange(eq(stay), eq(new BigDecimal("12000")),
                eq(new BigDecimal("15000")), eq(currentMonth));
    }

    @Test
    void retroactiveRentChangeIsRejected() {
        Rental rental = rental();
        when(rentalRepository.findById(12L)).thenReturn(Optional.of(rental));
        RentalUpdateRequest change = new RentalUpdateRequest(null, null, null, null, 5, null,
                List.of(new OccupantUpdateRequest(23L, LocalDate.of(2026, 1, 10),
                        new BigDecimal("15000"), LocalDate.now().withDayOfMonth(1).minusMonths(1))));
        assertThrows(com.rental.exception.BusinessRuleException.class, () -> service.update(12L, change));
        verify(tenantRentRateService, never()).recordChange(any(), any(), any(), any());
    }

    private Rental rental() {
        Tenant tenant = Tenant.builder().id(7L).build();
        Property property = Property.builder().id(8L).build();
        Rental rental = Rental.builder().id(12L).property(property).status(RentalStatus.ACTIVE)
                .paymentDueDay(5).startDate(LocalDate.of(2026, 1, 10))
                .monthlyRent(new BigDecimal("12000")).tenant(tenant)
                .occupants(new ArrayList<>()).build();
        rental.getOccupants().add(RentalTenant.builder().id(23L).rental(rental).tenant(tenant)
                .startDate(LocalDate.of(2026, 1, 10)).monthlyRent(new BigDecimal("12000")).build());
        return rental;
    }

    private RentPayment invoice(Rental rental, RentalTenant stay, LocalDate month, PaymentStatus status) {
        return RentPayment.builder().rental(rental).rentalTenant(stay).periodMonth(month)
                .amountDue(new BigDecimal("12000")).amountPaid(BigDecimal.ZERO)
                .dueDate(month.withDayOfMonth(5)).status(status).build();
    }

    private RentalUpdateRequest request(LocalDate start, String rent) {
        return new RentalUpdateRequest(null, null, null, null, 5, null,
                List.of(new OccupantUpdateRequest(23L, start, new BigDecimal(rent), LocalDate.now().withDayOfMonth(1))));
    }
}
