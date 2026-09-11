package com.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Backs the "Change Tenant" action: ends whatever rental is currently active on
 * the property (if any) and starts a new one for the incoming tenant,
 * atomically. Nothing about the previous rental is overwritten.
 */
public record ChangeTenantRequest(
        @NotNull(message = "New tenant is required")
        Long newTenantId,
        /**
         * Optional second tenant for properties that can accommodate 2
         * occupants
         */
        Long newTenant2Id,
        @NotNull(message = "New start date is required")
        LocalDate newStartDate,
        /**
         * Optional -- defaults to the outgoing rental's rent, or the property's
         * current rent if there was none
         */
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
