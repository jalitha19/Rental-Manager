package com.rental.service;

import com.rental.dto.*;
import com.rental.entity.Property;
import com.rental.entity.Rental;
import com.rental.entity.RentalTenant;
import com.rental.entity.RentPayment;
import com.rental.entity.Tenant;
import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.RentalStatus;
import com.rental.entity.enums.PaymentStatus;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
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
    private final TenantRentRateService tenantRentRateService;

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

        Rental saved = hasScheduledPeriods(occupantRequests)
                ? rentalRepository.saveAndFlush(rental) : rentalRepository.save(rental);
        persistNewSchedules(saved, occupantRequests);
        if (requestedStatus == RentalStatus.ACTIVE && hasScheduledPeriods(occupantRequests)) {
            generateMissingPayments(saved, occupantRequests);
        }

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

        List<RentalTenant> changedOccupants = new ArrayList<>();
        if (request.occupants() != null && !request.occupants().isEmpty()) {
            for (OccupantUpdateRequest change : request.occupants()) {
                RentalTenant occupant = occupants.stream()
                        .filter(o -> o.getId().equals(change.id()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Tenant entry not found on this rental: " + change.id()));
                boolean startChanged = !occupant.getStartDate().equals(change.startDate());
                boolean rentChanged = occupant.getMonthlyRent().compareTo(change.monthlyRent()) != 0;
                if (change.rentPeriods() != null && !change.rentPeriods().isEmpty()) {
                    tenantRentRateService.validatePeriods(change.startDate(), change.rentPeriods());
                    BigDecimal latestRate = change.rentPeriods().get(change.rentPeriods().size() - 1).monthlyRent();
                    if (latestRate.compareTo(change.monthlyRent()) != 0) {
                        throw new BusinessRuleException("Monthly rent must match the latest rent period");
                    }
                    occupant.setStartDate(change.startDate());
                    tenantRentRateService.replacePeriods(occupant, change.rentPeriods());
                    occupant.setMonthlyRent(latestRate);
                    // Never rewrite historical invoices. Current month's untouched invoice
                    // may reflect the newly confirmed current rent, as in the old workflow.
                    reconcileUnpaidPayments(rental, occupant, false, Boolean.TRUE.equals(change.generateMissingPayments()));
                } else {
                    if (rentChanged) applyRentChange(occupant, change.monthlyRent(), change.rentEffectiveMonth());
                    occupant.setStartDate(change.startDate());
                    if (startChanged || rentChanged) changedOccupants.add(occupant);
                }
            }
        } else {
            RentalTenant primary = occupants.get(0);
            if (request.startDate() != null) {
                primary.setStartDate(request.startDate());
            }
            if (request.monthlyRent() != null) {
                applyRentChange(primary, request.monthlyRent(), null);
            }
            if (request.startDate() != null || request.monthlyRent() != null) {
                changedOccupants.add(primary);
            }
        }

        if (rental.getStatus() != RentalStatus.ACTIVE
                && !Objects.equals(rental.getEndDate(), request.endDate())) {
            LocalDate previousRentalEnd = rental.getEndDate();
            rental.setEndDate(request.endDate());
            // Never move the individual departure dates of occupants who left earlier.
            for (RentalTenant occupant : occupants) {
                if (Objects.equals(occupant.getEndDate(), previousRentalEnd)) {
                    occupant.setEndDate(request.endDate());
                }
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
        for (RentalTenant occupant : changedOccupants) {
            reconcileUnpaidPayments(rental, occupant, false, true);
        }

        return rentalMapper.toResponse(rental);
    }

    /** Disallow retroactive changes: past invoices (even UNPAID) are immutable here. */
    private void applyRentChange(RentalTenant occupant, BigDecimal newRent, LocalDate effectiveMonth) {
        if (newRent == null || occupant.getMonthlyRent().compareTo(newRent) == 0) return;
        LocalDate currentMonth = YearMonth.now().atDay(1);
        LocalDate month = effectiveMonth == null ? currentMonth : effectiveMonth;
        if (!month.equals(currentMonth)) {
            throw new BusinessRuleException("New rent must take effect from the first day of the current month; prior months are protected");
        }
        // The current rent is updated only for this month onward; past rates stay in the ledger.
        tenantRentRateService.recordChange(occupant, occupant.getMonthlyRent(), newRent, month);
        occupant.setMonthlyRent(newRent);
    }

    /** Reconcile start-date changes, and only FUTURE/current unpaid invoices after a rent change. */
    private void reconcileUnpaidPayments(Rental rental, RentalTenant occupant, boolean preserveExisting, boolean generateMissing) {
        LocalDate firstMonth = occupant.getStartDate().withDayOfMonth(1);
        LocalDate lastMonth = YearMonth.now().atDay(1);
        LocalDate thisMonth = lastMonth;
        if (occupant.getEndDate() != null && occupant.getEndDate().withDayOfMonth(1).isBefore(lastMonth)) {
            lastMonth = occupant.getEndDate().withDayOfMonth(1);
        }
        List<RentPayment> payments = rentPaymentRepository.findByRentalIdOrderByPeriodMonthDesc(rental.getId());
        Set<LocalDate> existingMonths = new HashSet<>();
        for (RentPayment payment : payments) {
            if (payment.getRentalTenant() == null || !occupant.getId().equals(payment.getRentalTenant().getId())) continue;
            LocalDate month = payment.getPeriodMonth();
            existingMonths.add(month);
            boolean untouched = payment.getStatus() == PaymentStatus.UNPAID
                    && (payment.getAmountPaid() == null || payment.getAmountPaid().signum() == 0)
                    && payment.getPaymentDate() == null && payment.getPaymentMethod() == null
                    && (payment.getNotes() == null || payment.getNotes().isBlank());
            // Past payments must never be rewritten or deleted by editing tenant terms.
            if (!preserveExisting && untouched && !month.isBefore(thisMonth) && !month.isBefore(firstMonth)
                    && !month.isAfter(lastMonth)) {
                BigDecimal applicable = tenantRentRateService.rentFor(occupant, month);
                if (applicable != null) {
                    payment.setAmountDue(applicable);
                    payment.setDueDate(dueDateFor(rental, month));
                }
            }
        }
        if (rental.getStatus() != RentalStatus.ACTIVE || !generateMissing) return;
        for (LocalDate month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
            if (existingMonths.contains(month)) continue;
            BigDecimal rate = tenantRentRateService.rentFor(occupant, month);
            if (rate == null || rate.signum() <= 0) continue;
            rentPaymentRepository.save(RentPayment.builder()
                    .rental(rental).rentalTenant(occupant).periodMonth(month)
                    .amountDue(rate).amountPaid(BigDecimal.ZERO)
                    .dueDate(dueDateFor(rental, month)).status(PaymentStatus.UNPAID).build());
        }
    }

    private LocalDate dueDateFor(Rental rental, LocalDate month) {
        int dueDay = rental.getPaymentDueDay() != null ? rental.getPaymentDueDay() : 1;
        return month.withDayOfMonth(Math.min(dueDay, YearMonth.from(month).lengthOfMonth()));
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

        // If someone stays, update only the occupants who actually move in/out.
        // Do not close/recreate the continuing occupant's rental or payment identity.
        if (currentActive.isPresent()) {
            Rental current = currentActive.get();
            List<RentalTenant> continuing = current.getOccupants().stream()
                    .filter(o -> o.getEndDate() == null)
                    .filter(o -> occupantRequests.stream()
                            .anyMatch(requested -> requested.tenantId().equals(o.getTenant().getId())))
                    .toList();
            if (!continuing.isEmpty()) {
                List<OccupantRequest> newcomers = occupantRequests.stream()
                        .filter(requested -> continuing.stream()
                                .noneMatch(o -> o.getTenant().getId().equals(requested.tenantId())))
                        .toList();
                LocalDate effectiveDate = newcomers.stream()
                        .map(OccupantRequest::startDate)
                        .min(Comparator.naturalOrder())
                        .orElse(request.newStartDate());
                if (effectiveDate == null) {
                    throw new BusinessRuleException("Change date is required when removing an occupant");
                }
                LocalDate latestCurrentStart = latestCurrentStart(current);
                if (effectiveDate.isBefore(latestCurrentStart)) {
                    throw new BusinessRuleException("Change date cannot be before the current tenants' latest start date ("
                            + latestCurrentStart + ")");
                }
                // Do not allow an old/current occupant to be silently backdated or edited.
                // The existing RentalTenant row, original start date and rent remain unchanged.
                for (RentalTenant occupant : current.getOccupants()) {
                    if (occupant.getEndDate() == null && continuing.stream().noneMatch(
                            o -> o.getId().equals(occupant.getId()))) {
                        occupant.setEndDate(effectiveDate);
                    }
                }
                addOccupants(current, newcomers, property.getMonthlyRent(), null);
                if (current.getOccupants().stream().filter(o -> o.getEndDate() == null).count() > 2) {
                    throw new BusinessRuleException("A rental can have at most 2 current tenants");
                }
                // Leaving deposit/due day alone unless caller explicitly supplies updates.
                if (request.deposit() != null) current.setDeposit(request.deposit());
                if (request.paymentDueDay() != null) current.setPaymentDueDay(request.paymentDueDay());
                if (request.notes() != null && !request.notes().isBlank()) current.setNotes(request.notes());
                syncSummary(current);
                Rental savedCurrent = hasScheduledPeriods(newcomers)
                        ? rentalRepository.saveAndFlush(current) : rentalRepository.save(current);
                persistNewSchedules(savedCurrent, newcomers);
                if (hasScheduledPeriods(newcomers)) generateMissingPayments(savedCurrent, newcomers);
                // An explicitly edited continuing occupant keeps the same stay ID.
                // Update only its rate ledger; preserve all historical invoice amounts.
                for (RentalTenant staying : continuing) {
                    OccupantRequest update = occupantRequests.stream()
                            .filter(r -> r.tenantId().equals(staying.getTenant().getId()))
                            .findFirst().orElseThrow();
                    if (update.rentPeriods() != null && !update.rentPeriods().isEmpty()) {
                        tenantRentRateService.validatePeriods(staying.getStartDate(), update.rentPeriods());
                        tenantRentRateService.replacePeriods(staying, update.rentPeriods());
                        staying.setMonthlyRent(update.rentPeriods().get(update.rentPeriods().size() - 1).monthlyRent());
                        reconcileUnpaidPayments(current, staying, false, Boolean.TRUE.equals(update.generateMissingPayments()));
                    }
                }
                syncSummary(current);
                return rentalMapper.toResponse(savedCurrent);
            }
        }

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

        Rental saved = hasScheduledPeriods(occupantRequests)
                ? rentalRepository.saveAndFlush(incoming) : rentalRepository.save(incoming);
        persistNewSchedules(saved, occupantRequests);
        if (hasScheduledPeriods(occupantRequests)) generateMissingPayments(saved, occupantRequests);
        property.setStatus(PropertyStatus.OCCUPIED);

        return rentalMapper.toResponse(saved);
    }

    private boolean hasScheduledPeriods(List<OccupantRequest> requests) {
        return requests.stream().anyMatch(o -> o.rentPeriods() != null && !o.rentPeriods().isEmpty());
    }

    /** Save rate timelines only for explicitly supplied new occupants, not retained ones. */
    private void persistNewSchedules(Rental rental, List<OccupantRequest> requests) {
        for (OccupantRequest request : requests) {
            if (request.rentPeriods() == null || request.rentPeriods().isEmpty()) continue;
            RentalTenant occupant = rental.getOccupants().stream()
                    .filter(o -> o.getTenant().getId().equals(request.tenantId()))
                    .reduce((first, second) -> second)
                    .orElseThrow(() -> new BusinessRuleException("Cannot find newly assigned tenant"));
            tenantRentRateService.replacePeriods(occupant, request.rentPeriods());
        }
    }

    /** Historical schedules create only missing UNPAID invoices; never mark anything paid. */
    private void generateMissingPayments(Rental rental, List<OccupantRequest> requests) {
        for (OccupantRequest request : requests) {
            if (request.rentPeriods() == null || request.rentPeriods().isEmpty()
                    || !Boolean.TRUE.equals(request.generateMissingPayments())) continue;
            RentalTenant occupant = rental.getOccupants().stream()
                    .filter(o -> o.getTenant().getId().equals(request.tenantId()) && o.getEndDate() == null)
                    .findFirst().orElseThrow();
            reconcileUnpaidPayments(rental, occupant, true, true);
        }
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
            if (request.rentPeriods() != null && !request.rentPeriods().isEmpty()) {
                tenantRentRateService.validatePeriods(request.startDate(), request.rentPeriods());
                BigDecimal mostRecent = request.rentPeriods().get(request.rentPeriods().size() - 1).monthlyRent();
                if (request.monthlyRent() != null && request.monthlyRent().compareTo(mostRecent) != 0) {
                    throw new BusinessRuleException("Monthly rent must match the latest rent period");
                }
            }
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
                    .monthlyRent(request.rentPeriods() != null && !request.rentPeriods().isEmpty()
                            ? request.rentPeriods().get(request.rentPeriods().size() - 1).monthlyRent()
                            : request.monthlyRent() != null ? request.monthlyRent() : defaultRent)
                    .build());
        }
    }

    private void syncSummary(Rental rental) {
        List<RentalTenant> occupants = rental.getStatus() == RentalStatus.ACTIVE
                ? rental.getOccupants().stream().filter(o -> o.getEndDate() == null).toList()
                : rental.getOccupants();
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
