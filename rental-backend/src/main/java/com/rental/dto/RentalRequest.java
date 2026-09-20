package com.rental.dto;

import com.rental.entity.enums.RentalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Preferred way: send "occupants" (1 or 2 entries, each with its own tenant,
 * start date and monthly rent). The older fields tenantId, tenant2Id,
 * startDate and monthlyRent still work when "occupants" is not sent; in that
 * case the second tenant is created with a rent of 0 so the room is not
 * billed twice.
 */
public record RentalRequest(
        @NotNull(message = "Property is required")
        Long propertyId,
        Long tenantId,
        Long tenant2Id,
        LocalDate startDate,
        LocalDate endDate,
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,
        @DecimalMin(value = "0.0", message = "Deposit cannot be negative")
        BigDecimal deposit,
        @Min(value = 1, message = "Payment due day must be between 1 and 31")
        @Max(value = 31, message = "Payment due day must be between 1 and 31")
        Integer paymentDueDay,
        RentalStatus status,
        String notes,
        @Valid
        List<OccupantRequest> occupants
        ) {

}
