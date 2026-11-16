package com.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Optional rentPeriods let an incoming tenant be assigned a historical rate schedule. */
public record OccupantRequest(
        @NotNull(message = "Tenant is required") Long tenantId,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative") BigDecimal monthlyRent,
        @Valid List<RentPeriodRequest> rentPeriods,
        /** Explicit opt-in: create missing UNPAID invoices, not actual receipts. */
        Boolean generateMissingPayments
) {
    public OccupantRequest(Long tenantId, LocalDate startDate, BigDecimal monthlyRent) {
        this(tenantId, startDate, monthlyRent, null, false);
    }
    public OccupantRequest(Long tenantId, LocalDate startDate, BigDecimal monthlyRent,
                           List<RentPeriodRequest> rentPeriods) {
        this(tenantId, startDate, monthlyRent, rentPeriods, false);
    }
}
