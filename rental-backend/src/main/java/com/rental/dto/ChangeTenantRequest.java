package com.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Backs the "Change Tenant" action: ends whatever rental is currently active on
 * the property (if any) and starts a new one for the incoming tenants,
 * atomically. Nothing about the previous rental is overwritten.
 *
 * Preferred way: send "occupants" with a start date and rent for each person.
 * The older fields newTenantId, newTenant2Id, newStartDate and monthlyRent
 * still work when "occupants" is not sent.
 */
public record ChangeTenantRequest(
        Long newTenantId,
        Long newTenant2Id,
        LocalDate newStartDate,
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,
        @DecimalMin(value = "0.0", message = "Deposit cannot be negative")
        BigDecimal deposit,
        @Min(value = 1, message = "Payment due day must be between 1 and 31")
        @Max(value = 31, message = "Payment due day must be between 1 and 31")
        Integer paymentDueDay,
        String notes,
        @Valid
        List<OccupantRequest> occupants
        ) {

}
