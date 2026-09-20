package com.rental.mapper;

import com.rental.dto.RentPaymentResponse;
import com.rental.dto.RentalSummary;
import com.rental.entity.RentPayment;
import com.rental.entity.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RentPaymentMapper {

    public RentPaymentResponse toResponse(RentPayment payment) {
        return new RentPaymentResponse(
                payment.getId(),
                toRentalSummary(payment),
                payment.getPeriodMonth(),
                payment.getAmountDue(),
                payment.getAmountPaid(),
                payment.getDueDate(),
                payment.getPaymentDate(),
                payment.getStatus(),
                effectiveStatus(payment),
                payment.getPaymentMethod(),
                payment.getNotes(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getRentalTenant() != null ? payment.getRentalTenant().getId() : null
        );
    }

    /**
     * PAID stays PAID; otherwise becomes OVERDUE once the due date has passed,
     * without touching the stored value.
     */
    public PaymentStatus effectiveStatus(RentPayment payment) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return PaymentStatus.PAID;
        }
        if (payment.getDueDate() != null && payment.getDueDate().isBefore(LocalDate.now())) {
            return PaymentStatus.OVERDUE;
        }
        return payment.getStatus();
    }

    private RentalSummary toRentalSummary(RentPayment payment) {
        var rental = payment.getRental();
        var property = rental.getProperty();
        var tenant = payment.getRentalTenant() != null
                ? payment.getRentalTenant().getTenant()
                : rental.getTenant();

        return new RentalSummary(
                rental.getId(),
                new com.rental.dto.PropertySummary(property.getId(), property.getPropertyCode(), property.getName(), property.getType()),
                new com.rental.dto.TenantSummary(tenant.getId(), tenant.getFullName(), tenant.getTenantType(), tenant.getPhotoUrl())
        );
    }
}
