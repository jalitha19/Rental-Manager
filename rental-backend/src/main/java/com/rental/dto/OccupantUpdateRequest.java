package com.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OccupantUpdateRequest(
        @NotNull(message = "Tenant entry id is required")
        Long id,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent
        ) {

}
