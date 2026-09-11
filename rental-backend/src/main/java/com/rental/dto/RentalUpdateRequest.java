package com.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * For correcting details of an existing rental (e.g. a mistyped rent
 * amount). Status transitions are NOT done here -- use POST
 * /api/rentals/{id}/end to end a rental, or the change-tenant action to
 * swap occupants. This keeps those state transitions centrally validated.
 */
public record RentalUpdateRequest(
        @NotNull(message = "Start date is required")
        LocalDate startDate,

        /** Only meaningful for rentals that are already COMPLETED/CANCELLED */
        LocalDate endDate,

        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,

        @DecimalMin(value = "0.0", message = "Deposit cannot be negative")
        BigDecimal deposit,

        @Min(value = 1, message = "Payment due day must be between 1 and 31")
        @Max(value = 31, message = "Payment due day must be between 1 and 31")
        Integer paymentDueDay,

        String notes
) {
}
