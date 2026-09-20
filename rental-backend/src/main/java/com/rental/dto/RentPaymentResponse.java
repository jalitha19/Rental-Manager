package com.rental.dto;

import com.rental.entity.enums.PaymentMethod;
import com.rental.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RentPaymentResponse(
        Long id,
        RentalSummary rental,
        LocalDate periodMonth,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        LocalDate dueDate,
        LocalDate paymentDate,
        /** The stored status (PAID / PARTIALLY_PAID / UNPAID) */
        PaymentStatus status,
        /** Same as status, except UNPAID/PARTIALLY_PAID becomes OVERDUE once dueDate has passed */
        PaymentStatus effectiveStatus,
        PaymentMethod paymentMethod,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        /** The rental_tenants row this payment belongs to */
        Long rentalTenantId
) {
}
