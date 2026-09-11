package com.rental.service;

import com.rental.dto.DashboardResponse;
import com.rental.dto.RentPendingItem;
import com.rental.entity.Property;
import com.rental.entity.RentPayment;
import com.rental.entity.enums.PaymentStatus;
import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.PropertyType;
import com.rental.entity.enums.RentalStatus;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final RentalRepository rentalRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final RentPaymentService rentPaymentService;

    public DashboardResponse getDashboard() {
        // Make sure this month's payment rows exist for every active rental
        // before computing paid/unpaid counts -- keeps the dashboard accurate
        // without requiring a manual step at the start of each month.
        rentPaymentService.generateCurrentMonthPayments();

        List<Property> properties = propertyRepository.findAll();

        long totalHouses = properties.stream().filter(p -> p.getType() == PropertyType.HOUSE).count();
        long totalRooms = properties.stream().filter(p -> p.getType() == PropertyType.ROOM).count();
        long occupied = properties.stream().filter(p -> p.getStatus() == PropertyStatus.OCCUPIED).count();
        long available = properties.stream().filter(p -> p.getStatus() == PropertyStatus.AVAILABLE).count();
        long maintenance = properties.stream().filter(p -> p.getStatus() == PropertyStatus.MAINTENANCE).count();

        long roomsOccupied = properties.stream()
                .filter(p -> p.getType() == PropertyType.ROOM)
                .filter(p -> p.getStatus() == PropertyStatus.OCCUPIED)
                .count();
        long roomsAvailable = properties.stream()
                .filter(p -> p.getType() == PropertyType.ROOM)
                .filter(p -> p.getStatus() == PropertyStatus.AVAILABLE)
                .count();
        long housesOccupied = properties.stream()
                .filter(p -> p.getType() == PropertyType.HOUSE)
                .filter(p -> p.getStatus() == PropertyStatus.OCCUPIED)
                .count();
        long housesAvailable = properties.stream()
                .filter(p -> p.getType() == PropertyType.HOUSE)
                .filter(p -> p.getStatus() == PropertyStatus.AVAILABLE)
                .count();

        long totalTenants = tenantRepository.count();

        var activeRentals = rentalRepository.findByStatus(RentalStatus.ACTIVE);
        long roomsTenants = activeRentals.stream()
                .filter(r -> r.getProperty().getType() == PropertyType.ROOM)
                .mapToLong(r -> 1L + (r.getTenant2() != null ? 1L : 0L))
                .sum();
        long housesTenants = activeRentals.stream()
                .filter(r -> r.getProperty().getType() == PropertyType.HOUSE)
                .mapToLong(r -> 1L + (r.getTenant2() != null ? 1L : 0L))
                .sum();

        LocalDate periodMonth = YearMonth.now().atDay(1);
        List<RentPayment> thisMonth = rentPaymentRepository.findByPeriodMonth(periodMonth);

        long paidCount = thisMonth.stream().filter(p -> p.getStatus() == PaymentStatus.PAID).count();
        long unpaidCount = thisMonth.size() - paidCount;

        BigDecimal paidAmount = thisMonth.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(p -> p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaidAmount = thisMonth.stream()
                .filter(p -> p.getStatus() != PaymentStatus.PAID)
                .map(p -> p.getAmountDue().subtract(p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RentPendingItem> rentPending = thisMonth.stream()
                .filter(p -> p.getStatus() != PaymentStatus.PAID)
                .map(this::toRentPendingItem)
                .sorted((a, b) -> a.dueDate().compareTo(b.dueDate()))
                .toList();

        return new DashboardResponse(
                totalHouses,
                totalRooms,
                occupied,
                available,
                maintenance,
                totalTenants,
                paidCount,
                unpaidCount,
                paidAmount,
                unpaidAmount,
                rentPending,
                roomsOccupied,
                roomsAvailable,
                roomsTenants,
                housesOccupied,
                housesAvailable,
                housesTenants
        );
    }

    private RentPendingItem toRentPendingItem(RentPayment payment) {
        var rental = payment.getRental();
        var property = rental.getProperty();
        var tenant = rental.getTenant();
        var paid = payment.getAmountPaid() != null ? payment.getAmountPaid() : BigDecimal.ZERO;
        var balance = payment.getAmountDue().subtract(paid);
        boolean overdue = payment.getDueDate() != null && payment.getDueDate().isBefore(LocalDate.now());

        return new RentPendingItem(
                payment.getId(),
                rental.getId(),
                tenant.getFullName(),
                property.getPropertyCode(),
                property.getName(),
                payment.getAmountDue(),
                payment.getAmountPaid(),
                balance,
                payment.getDueDate(),
                overdue
        );
    }
}
