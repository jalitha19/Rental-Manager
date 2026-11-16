package com.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A monthly price interval; endMonth is inclusive, null means ongoing. */
public record RentPeriodRequest(
        @NotNull LocalDate startMonth,
        LocalDate endMonth,
        @NotNull @DecimalMin("0.0") BigDecimal monthlyRent
) {}
