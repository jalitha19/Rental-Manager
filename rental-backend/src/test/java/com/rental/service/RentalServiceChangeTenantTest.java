package com.rental.service;

import com.rental.dto.ChangeTenantRequest;
import com.rental.dto.OccupantRequest;
import com.rental.entity.Property;
import com.rental.entity.Rental;
import com.rental.entity.RentalTenant;
import com.rental.entity.Tenant;
import com.rental.entity.enums.RentalStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceChangeTenantTest {
    @Mock RentalRepository rentalRepository;
    @Mock RentPaymentRepository rentPaymentRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock TenantRepository tenantRepository;
    @Mock RentalMapper rentalMapper;
    @InjectMocks RentalService service;

    Property property;
    Rental existing;
    RentalTenant aliceStay;
    Tenant alice;
    Tenant bob;
    Tenant carol;
    LocalDate originalStart = LocalDate.of(2026, 1, 10);
    LocalDate changeDate = LocalDate.of(2026, 11, 16);

    @BeforeEach
    void setup() {
        property = Property.builder().id(9L).monthlyRent(new BigDecimal("15000")).build();
        alice = Tenant.builder().id(1L).fullName("Alice").build();
        bob = Tenant.builder().id(2L).fullName("Bob").build();
        carol = Tenant.builder().id(3L).fullName("Carol").build();
        aliceStay = RentalTenant.builder().id(22L).tenant(alice).startDate(originalStart)
                .monthlyRent(new BigDecimal("10000")).build();
        existing = Rental.builder().id(13L).property(property).status(RentalStatus.ACTIVE)
                .tenant(alice).startDate(originalStart).monthlyRent(new BigDecimal("10000"))
                .occupants(new ArrayList<>()).build();
        aliceStay.setRental(existing);
        existing.getOccupants().add(aliceStay);
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(property));
        when(rentalRepository.findByPropertyIdAndStatus(9L, RentalStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ChangeTenantRequest request(List<OccupantRequest> occupants) {
        return new ChangeTenantRequest(null, null, changeDate, null, null, null, null, occupants);
    }

    @Test
    void addingSecondPersonRetainsOriginalStayAndRental() {
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(bob));
        service.changeTenant(9L, request(List.of(
                new OccupantRequest(1L, originalStart, new BigDecimal("10000")),
                new OccupantRequest(2L, changeDate, new BigDecimal("6000")))));

        assertEquals(13L, existing.getId());
        assertEquals(RentalStatus.ACTIVE, existing.getStatus());
        assertSame(aliceStay, existing.getOccupants().get(0));
        assertNull(aliceStay.getEndDate());
        assertEquals(originalStart, aliceStay.getStartDate());
        assertEquals(new BigDecimal("10000"), aliceStay.getMonthlyRent());
        assertEquals(2, existing.getOccupants().size());
        assertEquals(changeDate, existing.getOccupants().get(1).getStartDate());
        verify(rentalRepository, never()).saveAndFlush(any());
        verifyNoInteractions(rentPaymentRepository);
    }

    @Test
    void replacingSecondPersonKeepsFirstAndEndsOnlySecond() {
        RentalTenant bobStay = RentalTenant.builder().id(23L).rental(existing).tenant(bob)
                .startDate(LocalDate.of(2026, 3, 1))
                .monthlyRent(new BigDecimal("5000")).build();
        existing.getOccupants().add(bobStay);
        when(tenantRepository.findById(3L)).thenReturn(Optional.of(carol));
        service.changeTenant(9L, request(List.of(
                new OccupantRequest(1L, originalStart, new BigDecimal("99999")),
                new OccupantRequest(3L, changeDate, new BigDecimal("7000")))));

        assertNull(aliceStay.getEndDate());
        assertEquals(new BigDecimal("10000"), aliceStay.getMonthlyRent());
        assertEquals(changeDate, bobStay.getEndDate());
        assertEquals(3, existing.getOccupants().size());
        assertEquals(3L, existing.getOccupants().get(2).getTenant().getId());
        assertEquals(new BigDecimal("17000"), existing.getMonthlyRent());
        assertEquals(1L, existing.getTenant().getId());
        assertEquals(3L, existing.getTenant2().getId());
        verifyNoInteractions(rentPaymentRepository);
    }
}
