package com.rental.service;

import com.rental.dto.MarkPaidRequest;
import com.rental.dto.RentPaymentRequest;
import com.rental.dto.RentPaymentResponse;
import com.rental.dto.RentPaymentUpdateRequest;
import com.rental.entity.Rental;
import com.rental.entity.RentPayment;
import com.rental.entity.RentalTenant;
import com.rental.entity.enums.PaymentStatus;
import com.rental.entity.enums.RentalStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ResourceNotFoundException;
import com.rental.mapper.RentPaymentMapper;
import com.rental.repository.RentPaymentRepository;
import com.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RentPaymentService {

    private final RentPaymentRepository rentPaymentRepository;
    private final RentalRepository rentalRepository;
    private final RentPaymentMapper rentPaymentMapper;

    public List<RentPaymentResponse> list(Long rentalId, LocalDate periodMonth, Integer year, PaymentStatus status) {
        List<RentPayment> payments;
        if (rentalId != null) {
            payments = rentPaymentRepository.findByRentalIdOrderByPeriodMonthDesc(rentalId);
        } else if (periodMonth != null && year == null) {
            payments = rentPaymentRepository.findByPeriodMonth(periodMonth);
        } else {
            payments = rentPaymentRepository.findAll();
        }

        return payments.stream()
                .filter(payment -> periodMonth == null || periodMonth.equals(payment.getPeriodMonth()))
                .filter(payment -> year == null || payment.getPeriodMonth().getYear() == year)
                .filter(payment -> status == null || payment.getStatus() == status)
                .sorted((a, b) -> b.getPeriodMonth().compareTo(a.getPeriodMonth()))
                .map(rentPaymentMapper::toResponse)
                .toList();
    }

    public RentPaymentResponse get(Long id) {
        return rentPaymentMapper.toResponse(getEntityOrThrow(id));
    }

    public void delete(Long id) {
        rentPaymentRepository.delete(getEntityOrThrow(id));
    }

    public List<RentPaymentResponse> rentalHistory(Long rentalId) {
        return rentPaymentRepository.findByRentalIdOrderByPeriodMonthDesc(rentalId).stream()
                .map(rentPaymentMapper::toResponse)
                .toList();
    }

    public RentPaymentResponse create(RentPaymentRequest request) {
        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found: " + request.rentalId()));

        RentalTenant occupant = resolveOccupant(rental, request.rentalTenantId());
        LocalDate normalizedMonth = request.periodMonth().withDayOfMonth(1);

        if (rentPaymentRepository.existsByRentalTenantIdAndPeriodMonth(occupant.getId(), normalizedMonth)) {
            throw new BusinessRuleException(
                    "A payment record for " + normalizedMonth + " already exists for this tenant");
        }

        RentPayment payment = RentPayment.builder()
                .rental(rental)
                .rentalTenant(occupant)
                .periodMonth(normalizedMonth)
                .amountDue(request.amountDue())
                .amountPaid(BigDecimal.ZERO)
                .dueDate(request.dueDate())
                .status(PaymentStatus.UNPAID)
                .notes(request.notes())
                .build();

        return rentPaymentMapper.toResponse(rentPaymentRepository.save(payment));
    }

    private RentalTenant resolveOccupant(Rental rental, Long rentalTenantId) {
        List<RentalTenant> occupants = rental.getOccupants();
        if (rentalTenantId != null) {
            return occupants.stream()
                    .filter(occupant -> occupant.getId().equals(rentalTenantId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("That tenant does not belong to this rental"));
        }
        if (occupants.size() == 1) {
            return occupants.get(0);
        }
        throw new BusinessRuleException(
                "This rental has more than one tenant. Choose which tenant the payment is for.");
    }

    public RentPaymentResponse update(Long id, RentPaymentUpdateRequest request) {
        RentPayment payment = getEntityOrThrow(id);

        BigDecimal amountPaid = request.amountPaid() != null ? request.amountPaid() : BigDecimal.ZERO;
        if (amountPaid.compareTo(request.amountDue()) > 0) {
            throw new BusinessRuleException("Amount paid cannot be more than amount due");
        }

        payment.setAmountDue(request.amountDue());
        payment.setAmountPaid(amountPaid);
        payment.setDueDate(request.dueDate());
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setNotes(request.notes());
        payment.setStatus(deriveStatus(request.amountDue(), amountPaid));

        return rentPaymentMapper.toResponse(payment);
    }

    public RentPaymentResponse markPaid(Long id, MarkPaidRequest request) {
        RentPayment payment = getEntityOrThrow(id);

        BigDecimal currentPaid = payment.getAmountPaid() != null ? payment.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal newPaid = request.amountPaid() != null ? request.amountPaid() : payment.getAmountDue().subtract(currentPaid);

        if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Amount paid cannot be negative");
        }

        BigDecimal totalPaid = accumulatePaidAmount(currentPaid, newPaid, payment.getAmountDue());
        if (totalPaid.compareTo(payment.getAmountDue()) > 0) {
            throw new BusinessRuleException("Amount paid cannot be more than amount due");
        }

        payment.setAmountPaid(totalPaid);
        payment.setPaymentDate(request.paymentDate() != null ? request.paymentDate() : LocalDate.now());
        payment.setPaymentMethod(request.paymentMethod());
        if (request.notes() != null) {
            payment.setNotes(request.notes());
        }
        payment.setStatus(deriveStatus(payment.getAmountDue(), totalPaid));

        return rentPaymentMapper.toResponse(payment);
    }

    public static BigDecimal accumulatePaidAmount(BigDecimal currentPaid, BigDecimal newPaid, BigDecimal amountDue) {
        BigDecimal existing = currentPaid == null ? BigDecimal.ZERO : currentPaid;
        BigDecimal incoming = newPaid == null ? BigDecimal.ZERO : newPaid;
        BigDecimal total = existing.add(incoming);
        if (total.compareTo(amountDue) > 0) {
            throw new BusinessRuleException("Amount paid cannot be more than amount due");
        }
        return total;
    }

    /**
     * Creates missing UNPAID payment records for every tenant of every ACTIVE
     * rental, from that tenant's own start month through the requested month
     * (or the month they left, if earlier). Tenants with a monthly rent of 0
     * are skipped. Safe to call repeatedly -- existing records are skipped.
     */
    public List<RentPaymentResponse> generateCurrentMonthPayments() {
        return generateCurrentMonthPayments(null);
    }

    public List<RentPaymentResponse> generateCurrentMonthPayments(LocalDate toMonth) {
        LocalDate upperMonth = toMonth != null ? toMonth.withDayOfMonth(1) : YearMonth.now().atDay(1);
        List<Rental> activeRentals = rentalRepository.findByStatus(RentalStatus.ACTIVE);

        List<RentPaymentResponse> generated = new ArrayList<>();

        for (Rental rental : activeRentals) {
            for (RentalTenant occupant : rental.getOccupants()) {
                if (occupant.getMonthlyRent() == null || occupant.getMonthlyRent().signum() <= 0) {
                    continue;
                }

                LocalDate firstMonth = occupant.getStartDate().withDayOfMonth(1);
                LocalDate lastMonth = upperMonth;
                if (occupant.getEndDate() != null) {
                    LocalDate endMonth = occupant.getEndDate().withDayOfMonth(1);
                    if (endMonth.isBefore(lastMonth)) {
                        lastMonth = endMonth;
                    }
                }

                for (LocalDate month : monthsBetweenInclusive(firstMonth, lastMonth)) {
                    if (rentPaymentRepository.existsByRentalTenantIdAndPeriodMonth(occupant.getId(), month)) {
                        continue;
                    }

                    RentPayment payment = RentPayment.builder()
                            .rental(rental)
                            .rentalTenant(occupant)
                            .periodMonth(month)
                            .amountDue(occupant.getMonthlyRent())
                            .amountPaid(BigDecimal.ZERO)
                            .dueDate(dueDateFor(rental, month))
                            .status(PaymentStatus.UNPAID)
                            .build();

                    generated.add(rentPaymentMapper.toResponse(rentPaymentRepository.save(payment)));
                }
            }
        }

        return generated;
    }

    public static List<LocalDate> monthsBetweenInclusive(LocalDate fromMonth, LocalDate toMonth) {
        if (fromMonth == null || toMonth == null || fromMonth.isAfter(toMonth)) {
            return List.of();
        }

        List<LocalDate> months = new ArrayList<>();
        LocalDate cursor = fromMonth.withDayOfMonth(1);
        LocalDate end = toMonth.withDayOfMonth(1);

        while (!cursor.isAfter(end)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }

        return months;
    }

    private LocalDate dueDateFor(Rental rental, LocalDate periodMonth) {
        int day = rental.getPaymentDueDay() != null ? rental.getPaymentDueDay() : 1;
        int safeDay = Math.min(day, YearMonth.from(periodMonth).lengthOfMonth());
        return periodMonth.withDayOfMonth(safeDay);
    }

    private PaymentStatus deriveStatus(BigDecimal amountDue, BigDecimal amountPaid) {
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.UNPAID;
        }
        if (amountPaid.compareTo(amountDue) >= 0) {
            return PaymentStatus.PAID;
        }
        return PaymentStatus.PARTIALLY_PAID;
    }

    private RentPayment getEntityOrThrow(Long id) {
        return rentPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }
}
