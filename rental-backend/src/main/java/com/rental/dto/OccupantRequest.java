package com.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One tenant inside a rental, with their own start date and monthly rent.
 * If monthlyRent is omitted, the property's standard rent is used.
 */
public record OccupantRequest(
        @NotNull(message = "Tenant is required")
        Long tenantId,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent
        ) {

}
