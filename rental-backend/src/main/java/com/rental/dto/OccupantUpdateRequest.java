package com.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OccupantUpdateRequest(
        @NotNull(message = "Tenant entry id is required") Long id,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative") BigDecimal monthlyRent,
        LocalDate rentEffectiveMonth,
        @Valid List<RentPeriodRequest> rentPeriods,
        Boolean generateMissingPayments
) {
    public OccupantUpdateRequest(Long id, LocalDate startDate, BigDecimal monthlyRent, LocalDate rentEffectiveMonth) {
        this(id, startDate, monthlyRent, rentEffectiveMonth, null, false);
    }
    public OccupantUpdateRequest(Long id, LocalDate startDate, BigDecimal monthlyRent,
                                 LocalDate rentEffectiveMonth, List<RentPeriodRequest> rentPeriods) {
        this(id, startDate, monthlyRent, rentEffectiveMonth, rentPeriods, false);
    }
}
