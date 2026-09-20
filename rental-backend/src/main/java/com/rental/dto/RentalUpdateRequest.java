package com.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * For correcting details of an existing rental (e.g. a mistyped rent
 * amount). Status transitions are NOT done here -- use POST
 * /api/rentals/{id}/end to end a rental, or the change-tenant action to
 * swap occupants. This keeps those state transitions centrally validated.
 *
 * Preferred way: send "occupants" with the id, start date and rent of each
 * tenant entry. If it is not sent, startDate and monthlyRent are applied to
 * the first tenant only.
 */
public record RentalUpdateRequest(
        LocalDate startDate,

        /** Only meaningful for rentals that are already COMPLETED/CANCELLED */
        LocalDate endDate,

        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,

        @DecimalMin(value = "0.0", message = "Deposit cannot be negative")
        BigDecimal deposit,

        @Min(value = 1, message = "Payment due day must be between 1 and 31")
        @Max(value = 31, message = "Payment due day must be between 1 and 31")
        Integer paymentDueDay,

        String notes,

        @Valid
        List<OccupantUpdateRequest> occupants
) {
}
