package com.rental.service;

import com.rental.entity.*;
import com.rental.entity.enums.RentalStatus;
import com.rental.mapper.RentPaymentMapper;
import com.rental.repository.RentPaymentRepository;
import com.rental.repository.RentalRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentPaymentServiceGapTest {
    @Mock RentPaymentRepository rentPaymentRepository;
    @Mock RentalRepository rentalRepository;
    @Mock RentPaymentMapper rentPaymentMapper;
    @Mock TenantRentRateService tenantRentRateService;
    @InjectMocks RentPaymentService service;

    @Test
    void fullyUnpricedGapMonthDoesNotInventAnUnpaidInvoice() {
        LocalDate month = YearMonth.now().atDay(1);
        Rental rental = Rental.builder().id(22L).status(RentalStatus.ACTIVE)
                .paymentDueDay(5).occupants(new ArrayList<>()).build();
        RentalTenant occupant = RentalTenant.builder().id(19L).rental(rental)
                .startDate(month).monthlyRent(new BigDecimal("7000")).build();
        rental.getOccupants().add(occupant);
        when(rentalRepository.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of(rental));
        when(tenantRentRateService.rentFor(occupant, month)).thenReturn(null);

        assertTrue(service.generateCurrentMonthPayments(month).isEmpty());
        verify(rentPaymentRepository, never()).save(any(RentPayment.class));
    }
}
