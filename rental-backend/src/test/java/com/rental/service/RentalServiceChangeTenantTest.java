package com.rental.service;

import com.rental.dto.ChangeTenantRequest;
import com.rental.dto.OccupantRequest;
import com.rental.dto.RentPeriodRequest;
import com.rental.exception.BusinessRuleException;
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
    @Mock TenantRentRateService tenantRentRateService;
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
        lenient().when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
    void historicalMoveInBeforeOutgoingStartDoesNotBackdateTheMoveOut() {
        // Reproduces the reported case: an incoming tenant's rental history
        // begins 24 Jan 2019, while the outgoing tenant arrived 18 Mar 2019.
        LocalDate outgoingStart = LocalDate.of(2019, 3, 18);
        LocalDate historicalStart = LocalDate.of(2019, 1, 24);
        aliceStay.setStartDate(outgoingStart);
        existing.setStartDate(outgoingStart);
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(rentalRepository.saveAndFlush(existing)).thenReturn(existing);
        var incoming = service.changeTenant(9L, request(List.of(
                new OccupantRequest(2L, historicalStart, new BigDecimal("7000")))));

        assertEquals(changeDate, aliceStay.getEndDate());
        assertEquals(changeDate, existing.getEndDate());
        assertEquals(RentalStatus.COMPLETED, existing.getStatus());
        var savedRental = org.mockito.ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(savedRental.capture());
        assertEquals(historicalStart, savedRental.getValue().getStartDate());
        assertEquals(RentalStatus.ACTIVE, savedRental.getValue().getStatus());
        verifyNoInteractions(rentPaymentRepository);
    }

    @Test
    void fullReplacementWithHistoricalRatePeriodsDoesNotEndOldStayIn2019() {
        LocalDate originalOutgoingStart = LocalDate.of(2019, 3, 18);
        LocalDate backfilledStart = LocalDate.of(2019, 1, 24);
        aliceStay.setStartDate(originalOutgoingStart);
        existing.setStartDate(originalOutgoingStart);
        var periods = List.of(
                new RentPeriodRequest(backfilledStart, LocalDate.of(2022, 10, 26), new BigDecimal("3500")),
                new RentPeriodRequest(LocalDate.of(2022, 10, 27), null, new BigDecimal("7000")));
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(rentalRepository.saveAndFlush(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.changeTenant(9L, request(List.of(
                new OccupantRequest(2L, backfilledStart, new BigDecimal("7000"), periods))));

        assertEquals(changeDate, aliceStay.getEndDate());
        assertEquals(RentalStatus.COMPLETED, existing.getStatus());
        var captor = org.mockito.ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository, times(2)).saveAndFlush(captor.capture());
        Rental incoming = captor.getAllValues().get(1);
        assertEquals(backfilledStart, incoming.getStartDate());
        assertEquals(RentalStatus.ACTIVE, incoming.getStatus());
        verify(tenantRentRateService).replacePeriods(incoming.getOccupants().get(0), periods);
        verifyNoInteractions(rentPaymentRepository);
    }

    @Test
    void addingBackdatedOccupantKeepsExistingTenantAndRateHistory() {
        LocalDate historicalStart = LocalDate.of(2019, 1, 24);
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(rentalRepository.saveAndFlush(existing)).thenReturn(existing);
        var schedule = List.of(
                new RentPeriodRequest(historicalStart, LocalDate.of(2022, 10, 26), new BigDecimal("3500")),
                new RentPeriodRequest(LocalDate.of(2022, 10, 27), null, new BigDecimal("7000")));
        service.changeTenant(9L, request(List.of(
                new OccupantRequest(1L, originalStart, new BigDecimal("10000")),
                new OccupantRequest(2L, historicalStart, new BigDecimal("7000"), schedule))));

        assertNull(aliceStay.getEndDate());
        assertEquals(originalStart, aliceStay.getStartDate());
        assertEquals(new BigDecimal("10000"), aliceStay.getMonthlyRent());
        assertEquals(historicalStart, existing.getOccupants().get(1).getStartDate());
        verify(tenantRentRateService).replacePeriods(eq(existing.getOccupants().get(1)), eq(schedule));
        verifyNoInteractions(rentPaymentRepository);
    }

    @Test
    void anActualChangeoverCannotPrecedeTheOutgoingTenantMoveIn() {
        LocalDate outgoingStart = LocalDate.of(2026, 12, 1);
        aliceStay.setStartDate(outgoingStart);
        existing.setStartDate(outgoingStart);
        var exception = assertThrows(BusinessRuleException.class, () ->
                service.changeTenant(9L, request(List.of(
                        new OccupantRequest(2L, LocalDate.of(2019, 1, 24), new BigDecimal("7000"))))));
        assertTrue(exception.getMessage().contains("Changeover date"));
        assertNull(aliceStay.getEndDate());
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
