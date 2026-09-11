package com.rental.dto;

import com.rental.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * For fixing a payment entered incorrectly. `status` is deliberately not
 * part of this DTO -- it's always derived from amountDue vs amountPaid
 * (PAID / PARTIALLY_PAID / UNPAID), and OVERDUE is computed at read time
 * from the due date. This keeps status consistent instead of letting it
 * drift from the actual numbers.
 */
public record RentPaymentUpdateRequest(
        @NotNull(message = "Amount due is required")
        @DecimalMin(value = "0.0", message = "Amount due cannot be negative")
        BigDecimal amountDue,

        @DecimalMin(value = "0.0", message = "Amount paid cannot be negative")
        BigDecimal amountPaid,

        @NotNull(message = "Due date is required")
        LocalDate dueDate,

        LocalDate paymentDate,

        PaymentMethod paymentMethod,

        String notes
) {
}
