package com.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentPaymentRequest(
        @NotNull(message = "Rental is required")
        Long rentalId,

        @NotNull(message = "Period month is required")
        LocalDate periodMonth,

        @NotNull(message = "Amount due is required")
        @DecimalMin(value = "0.0", message = "Amount due cannot be negative")
        BigDecimal amountDue,

        @NotNull(message = "Due date is required")
        LocalDate dueDate,

        String notes
) {
}
