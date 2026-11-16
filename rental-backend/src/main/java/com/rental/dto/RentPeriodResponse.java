package com.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Exact dates plus legacy month fields for deployed frontend compatibility. */
public record RentPeriodResponse(
        LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent,
        LocalDate startMonth, LocalDate endMonth
) {
    public RentPeriodResponse(LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent) {
        this(startDate, endDate, monthlyRent,
                startDate == null ? null : startDate.withDayOfMonth(1),
                endDate == null ? null : endDate.withDayOfMonth(1));
    }
}
