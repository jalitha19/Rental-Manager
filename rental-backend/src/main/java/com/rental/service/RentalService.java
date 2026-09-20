package com.rental.service;

import com.rental.dto.*;
import com.rental.entity.Property;
import com.rental.entity.Rental;
import com.rental.entity.RentalTenant;
import com.rental.entity.Tenant;
import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.RentalStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ResourceNotFoundException;
import com.rental.mapper.RentalMapper;
import com.rental.repository.PropertyRepository;
import com.rental.repository.RentPaymentRepository;
import com.rental.repository.RentalRepository;
import com.rental.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalService {

    private final RentalRepository rentalRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final RentalMapper rentalMapper;

    public List<RentalResponse> list(Long propertyId, Long tenantId, RentalStatus status) {
        return rentalRepository.findAll().stream()
                .filter(rental -> propertyId == null || rental.getProperty().getId().equals(propertyId))
                .filter(rental -> tenantId == null || rental.getOccupants().stream()
                        .anyMatch(occupant -> occupant.getTenant().getId().equals(tenantId)))
                .filter(rental -> status == null || rental.getStatus() == status)
                .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                .map(rentalMapper::toResponse)
                .toList();
    }

    public RentalResponse get(Long id) {
        return rentalMapper.toResponse(getEntityOrThrow(id));
    }

    public void delete(Long id) {
        Rental rental = getEntityOrThrow(id);
        if (rental.getStatus() == RentalStatus.ACTIVE) {
            throw new BusinessRuleException("End an active rental before deleting it");
        }

        Property property = rental.getProperty();
        rentPaymentRepository.deleteByRentalId(rental.getId());
        rentalRepository.delete(rental);
        if (property.getStatus() == PropertyStatus.OCCUPIED) {
            property.setStatus(PropertyStatus.AVAILABLE);
        }
    }

    public List<RentalResponse> propertyHistory(Long propertyId) {
        ensurePropertyExists(propertyId);
        return rentalRepository.findByPropertyIdOrderByStartDateDesc(propertyId).stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    public List<RentalResponse> tenantHistory(Long tenantId) {
        ensureTenantExists(tenantId);
        return rentalRepository.findByOccupantTenantId(tenantId).stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    /**
     * Creates a rental on a property that currently has no active occupant.
     * Historical records can be persisted as COMPLETED/CANCELLED by sending an
     * explicit status and optional end date.
     */
    public RentalResponse create(RentalRequest request) {
        Property property = getPropertyOrThrow(request.propertyId());
        List<OccupantRequest> occupantRequests = resolveOccupants(
                request.occupants(), request.tenantId(), request.tenant2Id(),
                request.startDate(), request.monthlyRent());
        validateOccupantRequests(occupantRequests);

        RentalStatus requestedStatus = request.status() == null ? RentalStatus.ACTIVE : request.status();

        if (requestedStatus == RentalStatus.ACTIVE
                && rentalRepository.findByPropertyIdAndStatus(property.getId(), RentalStatus.ACTIVE).isPresent()) {
            throw new BusinessRuleException(
                    "This property already has an active rental. Use \"Change Tenant\" to move a new tenant in.");
        }

        Rental rental = Rental.builder()
                .property(property)
                .endDate(request.endDate())
                .deposit(request.deposit())
                .paymentDueDay(request.paymentDueDay())
                .status(requestedStatus)
                .notes(request.notes())
                .build();

        addOccupants(rental, occupantRequests, property.getMonthlyRent(), request.endDate());
        syncSummary(rental);

        if (request.endDate() != null && request.endDate().isBefore(latestStart(rental.getOccupants()))) {
            throw new BusinessRuleException("End date cannot be before start date");
        }

        Rental saved = rentalRepository.save(rental);

        if (requestedStatus == RentalStatus.ACTIVE) {
            property.setStatus(PropertyStatus.OCCUPIED);
        }

        return rentalMapper.toResponse(saved);
    }

    /**
     * Edits details of an existing rental. Does not change tenant, property, or
     * status. Start date and rent are edited per tenant entry.
     */
    public RentalResponse update(Long id, RentalUpdateRequest request) {
        Rental rental = getEntityOrThrow(id);

        if (rental.getStatus() == RentalStatus.ACTIVE && request.endDate() != null) {
            throw new BusinessRuleException(
                    "An active rental can't have an end date set directly. "
                    + "Use \"End Rental\" or \"Change Tenant\" instead.");
        }

        List<RentalTenant> occupants = rental.getOccupants();
        if (occupants.isEmpty()) {
            throw new BusinessRuleException("This rental has no tenants recorded");
        }

        if (request.occupants() != null && !request.occupants().isEmpty()) {
            for (OccupantUpdateRequest change : request.occupants()) {
                RentalTenant occupant = occupants.stream()
                        .filter(o -> o.getId().equals(change.id()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Tenant entry not found on this rental: " + change.id()));
                occupant.setStartDate(change.startDate());
                occupant.setMonthlyRent(change.monthlyRent());
            }
        } else {
            RentalTenant primary = occupants.get(0);
            if (request.startDate() != null) {
                primary.setStartDate(request.startDate());
            }
            if (request.monthlyRent() != null) {
                primary.setMonthlyRent(request.monthlyRent());
            }
        }

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            rental.setEndDate(request.endDate());
            for (RentalTenant occupant : occupants) {
                occupant.setEndDate(request.endDate());
            }
        }

        for (RentalTenant occupant : occupants) {
            if (occupant.getEndDate() != null && occupant.getEndDate().isBefore(occupant.getStartDate())) {
                throw new BusinessRuleException("End date cannot be before start date");
            }
        }

        rental.setDeposit(request.deposit());
        rental.setPaymentDueDay(request.paymentDueDay());
        rental.setNotes(request.notes());
        syncSummary(rental);

        return rentalMapper.toResponse(rental);
    }

    /**
     * Ends (or cancels) an active rental with no new tenant lined up yet --
     * property becomes AVAILABLE.
     */
    public RentalResponse end(Long id, EndRentalRequest request) {
        Rental rental = getEntityOrThrow(id);

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new BusinessRuleException("Only an active rental can be ended");
        }
        LocalDate latestCurrentStart = latestCurrentStart(rental);
        if (request.endDate().isBefore(latestCurrentStart)) {
            throw new BusinessRuleException(
                    "End date cannot be before the latest tenant start date (" + latestCurrentStart + ")");
        }
        if (request.status() == RentalStatus.ACTIVE) {
            throw new BusinessRuleException("Status must be COMPLETED or CANCELLED");
        }

        rental.setEndDate(request.endDate());
        rental.setStatus(request.status());
        for (RentalTenant occupant : rental.getOccupants()) {
            if (occupant.getEndDate() == null) {
                occupant.setEndDate(request.endDate());
            }
        }
        if (request.notes() != null) {
            rental.setNotes(request.notes());
        }

        Property property = rental.getProperty();
        property.setStatus(PropertyStatus.AVAILABLE);

        return rentalMapper.toResponse(rental);
    }

    /**
     * The core "Change Tenant" action (spec Section 7): ends whatever rental is
     * currently active on the property (if any) and creates a new active rental
     * for the incoming tenants, in one atomic operation. The outgoing rental is
     * never deleted or overwritten -- only its endDate/status change.
     */
    public RentalResponse changeTenant(Long propertyId, ChangeTenantRequest request) {
        Property property = getPropertyOrThrow(propertyId);
        List<OccupantRequest> occupantRequests = resolveOccupants(
                request.occupants(), request.newTenantId(), request.newTenant2Id(),
                request.newStartDate(), request.monthlyRent());
        validateOccupantRequests(occupantRequests);

        LocalDate changeDate = occupantRequests.stream()
                .map(OccupantRequest::startDate)
                .min(Comparator.naturalOrder())
                .orElseThrow();

        Optional<Rental> currentActive = rentalRepository.findByPropertyIdAndStatus(propertyId, RentalStatus.ACTIVE);

        BigDecimal defaultRent = property.getMonthlyRent();
        BigDecimal deposit = request.deposit();
        Integer paymentDueDay = request.paymentDueDay();

        if (currentActive.isPresent()) {
            Rental outgoing = currentActive.get();

            LocalDate latestCurrentStart = latestCurrentStart(outgoing);
            if (changeDate.isBefore(latestCurrentStart)) {
                throw new BusinessRuleException(
                        "New start date cannot be before the current tenants' latest start date ("
                        + latestCurrentStart + ")");
            }

            if (outgoing.getOccupants().size() == 1) {
                defaultRent = outgoing.getOccupants().get(0).getMonthlyRent();
            }

            for (RentalTenant occupant : outgoing.getOccupants()) {
                if (occupant.getEndDate() == null) {
                    occupant.setEndDate(changeDate);
                }
            }
            outgoing.setEndDate(changeDate);
            outgoing.setStatus(RentalStatus.COMPLETED);
            rentalRepository.saveAndFlush(outgoing);

            if (deposit == null) {
                deposit = outgoing.getDeposit();
            }
            if (paymentDueDay == null) {
                paymentDueDay = outgoing.getPaymentDueDay();
            }
        }

        Rental incoming = Rental.builder()
                .property(property)
                .deposit(deposit)
                .paymentDueDay(paymentDueDay)
                .status(RentalStatus.ACTIVE)
                .notes(request.notes())
                .build();

        addOccupants(incoming, occupantRequests, defaultRent, null);
        syncSummary(incoming);

        Rental saved = rentalRepository.save(incoming);
        property.setStatus(PropertyStatus.OCCUPIED);

        return rentalMapper.toResponse(saved);
    }

    private List<OccupantRequest> resolveOccupants(
            List<OccupantRequest> occupants,
            Long tenantId,
            Long tenant2Id,
            LocalDate startDate,
            BigDecimal monthlyRent
    ) {
        if (occupants != null && !occupants.isEmpty()) {
            return occupants;
        }
        if (tenantId == null) {
            throw new BusinessRuleException("Tenant is required");
        }
        if (startDate == null) {
            throw new BusinessRuleException("Start date is required");
        }
        List<OccupantRequest> resolved = new ArrayList<>();
        resolved.add(new OccupantRequest(tenantId, startDate, monthlyRent));
        if (tenant2Id != null) {
            resolved.add(new OccupantRequest(tenant2Id, startDate, BigDecimal.ZERO));
        }
        return resolved;
    }

    private void validateOccupantRequests(List<OccupantRequest> requests) {
        if (requests.size() > 2) {
            throw new BusinessRuleException("A rental can have at most 2 tenants");
        }
        Set<Long> seen = new HashSet<>();
        for (OccupantRequest request : requests) {
            if (!seen.add(request.tenantId())) {
                throw new BusinessRuleException("The same tenant cannot be added twice to one rental");
            }
        }
    }

    private void addOccupants(Rental rental, List<OccupantRequest> requests, BigDecimal defaultRent, LocalDate endDate) {
        for (OccupantRequest request : requests) {
            rental.getOccupants().add(RentalTenant.builder()
                    .rental(rental)
                    .tenant(getTenantOrThrow(request.tenantId()))
                    .startDate(request.startDate())
                    .endDate(endDate)
                    .monthlyRent(request.monthlyRent() != null ? request.monthlyRent() : defaultRent)
                    .build());
        }
    }

    private void syncSummary(Rental rental) {
        List<RentalTenant> occupants = rental.getOccupants();
        rental.setTenant(occupants.get(0).getTenant());
        rental.setTenant2(occupants.size() > 1 ? occupants.get(1).getTenant() : null);
        rental.setStartDate(occupants.stream()
                .map(RentalTenant::getStartDate)
                .min(Comparator.naturalOrder())
                .orElseThrow());
        rental.setMonthlyRent(occupants.stream()
                .map(RentalTenant::getMonthlyRent)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private LocalDate latestStart(List<RentalTenant> occupants) {
        return occupants.stream()
                .map(RentalTenant::getStartDate)
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    private LocalDate latestCurrentStart(Rental rental) {
        return rental.getOccupants().stream()
                .filter(occupant -> occupant.getEndDate() == null)
                .map(RentalTenant::getStartDate)
                .max(Comparator.naturalOrder())
                .orElse(rental.getStartDate());
    }

    private void ensurePropertyExists(Long id) {
        if (!propertyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Property not found: " + id);
        }
    }

    private void ensureTenantExists(Long id) {
        if (!tenantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tenant not found: " + id);
        }
    }

    private Property getPropertyOrThrow(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));
    }

    private Tenant getTenantOrThrow(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }

    private Rental getEntityOrThrow(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found: " + id));
    }
}
