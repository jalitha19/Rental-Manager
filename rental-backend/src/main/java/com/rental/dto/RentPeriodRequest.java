package com.rental.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Inclusive exact calendar dates; older clients may still send startMonth/endMonth. */
public record RentPeriodRequest(
        @JsonAlias("startMonth") @NotNull LocalDate startDate,
        @JsonAlias("endMonth") LocalDate endDate,
        @NotNull @DecimalMin("0.0") BigDecimal monthlyRent
) {
    // Existing Java API compatibility (old tests/integrations).
    public LocalDate startMonth() { return startDate; }
    public LocalDate endMonth() { return endDate; }
}
