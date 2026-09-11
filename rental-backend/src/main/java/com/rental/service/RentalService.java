package com.rental.service;

import com.rental.dto.*;
import com.rental.entity.Property;
import com.rental.entity.Rental;
import com.rental.entity.Tenant;
import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.RentalStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ResourceNotFoundException;
import com.rental.mapper.RentalMapper;
import com.rental.repository.PropertyRepository;
import com.rental.repository.RentalRepository;
import com.rental.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalService {

    private final RentalRepository rentalRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final RentalMapper rentalMapper;

    public List<RentalResponse> list(Long propertyId, Long tenantId, RentalStatus status) {
        return rentalRepository.findAll().stream()
                .filter(rental -> propertyId == null || rental.getProperty().getId().equals(propertyId))
                .filter(rental -> tenantId == null || rental.getTenant().getId().equals(tenantId))
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
        return rentalRepository.findByTenantIdOrderByStartDateDesc(tenantId).stream()
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
        Tenant tenant = getTenantOrThrow(request.tenantId());
        Tenant tenant2 = request.tenant2Id() != null ? getTenantOrThrow(request.tenant2Id()) : null;

        RentalStatus requestedStatus = request.status() == null ? RentalStatus.ACTIVE : request.status();

        if (requestedStatus == RentalStatus.ACTIVE
                && rentalRepository.findByPropertyIdAndStatus(property.getId(), RentalStatus.ACTIVE).isPresent()) {
            throw new BusinessRuleException(
                    "This property already has an active rental. Use \"Change Tenant\" to move a new tenant in.");
        }

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("End date cannot be before start date");
        }

        Rental rental = Rental.builder()
                .property(property)
                .tenant(tenant)
                .tenant2(tenant2)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .monthlyRent(request.monthlyRent() != null ? request.monthlyRent() : property.getMonthlyRent())
                .deposit(request.deposit())
                .paymentDueDay(request.paymentDueDay())
                .status(requestedStatus)
                .notes(request.notes())
                .build();

        Rental saved = rentalRepository.save(rental);

        if (requestedStatus == RentalStatus.ACTIVE) {
            property.setStatus(PropertyStatus.OCCUPIED);
        }

        return rentalMapper.toResponse(saved);
    }

    /**
     * Edits details of an existing rental. Does not change tenant, property, or
     * status.
     */
    public RentalResponse update(Long id, RentalUpdateRequest request) {
        Rental rental = getEntityOrThrow(id);

        if (rental.getStatus() == RentalStatus.ACTIVE && request.endDate() != null) {
            throw new BusinessRuleException(
                    "An active rental can't have an end date set directly. "
                    + "Use \"End Rental\" or \"Change Tenant\" instead.");
        }

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("End date cannot be before start date");
        }

        rental.setStartDate(request.startDate());
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            rental.setEndDate(request.endDate());
        }
        rental.setMonthlyRent(request.monthlyRent());
        rental.setDeposit(request.deposit());
        rental.setPaymentDueDay(request.paymentDueDay());
        rental.setNotes(request.notes());

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
        if (request.endDate().isBefore(rental.getStartDate())) {
            throw new BusinessRuleException("End date cannot be before the rental's start date");
        }
        if (request.status() == RentalStatus.ACTIVE) {
            throw new BusinessRuleException("Status must be COMPLETED or CANCELLED");
        }

        rental.setEndDate(request.endDate());
        rental.setStatus(request.status());
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
     * for the incoming tenant, in one atomic operation. The outgoing rental is
     * never deleted or overwritten -- only its endDate/status change.
     */
    public RentalResponse changeTenant(Long propertyId, ChangeTenantRequest request) {
        Property property = getPropertyOrThrow(propertyId);
        Tenant newTenant = getTenantOrThrow(request.newTenantId());
        Tenant newTenant2 = request.newTenant2Id() != null ? getTenantOrThrow(request.newTenant2Id()) : null;

        Optional<Rental> currentActive = rentalRepository.findByPropertyIdAndStatus(propertyId, RentalStatus.ACTIVE);

        var monthlyRent = request.monthlyRent();
        var deposit = request.deposit();
        var paymentDueDay = request.paymentDueDay();

        if (currentActive.isPresent()) {
            Rental outgoing = currentActive.get();

            if (request.newStartDate().isBefore(outgoing.getStartDate())) {
                throw new BusinessRuleException(
                        "New start date cannot be before the current tenant's start date ("
                        + outgoing.getStartDate() + ")");
            }

            outgoing.setEndDate(request.newStartDate());
            outgoing.setStatus(RentalStatus.COMPLETED);
            rentalRepository.saveAndFlush(outgoing);

            if (monthlyRent == null) {
                monthlyRent = outgoing.getMonthlyRent();
            }
            if (deposit == null) {
                deposit = outgoing.getDeposit();
            }
            if (paymentDueDay == null) {
                paymentDueDay = outgoing.getPaymentDueDay();
            }
        } else {
            if (monthlyRent == null) {
                monthlyRent = property.getMonthlyRent();
            }
        }

        Rental incoming = Rental.builder()
                .property(property)
                .tenant(newTenant)
                .tenant2(newTenant2)
                .startDate(request.newStartDate())
                .monthlyRent(monthlyRent)
                .deposit(deposit)
                .paymentDueDay(paymentDueDay)
                .status(RentalStatus.ACTIVE)
                .notes(request.notes())
                .build();

        Rental saved = rentalRepository.save(incoming);
        property.setStatus(PropertyStatus.OCCUPIED);

        return rentalMapper.toResponse(saved);
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
