package com.rental.dto;

import com.rental.entity.enums.RentalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentalRequest(
        @NotNull(message = "Property is required")
        Long propertyId,
        @NotNull(message = "Tenant is required")
        Long tenantId,
        /**
         * Optional second tenant for properties that can accommodate 2
         * occupants
         */
        Long tenant2Id,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        LocalDate endDate,
        /**
         * Optional -- defaults to the property's current monthly rent if
         * omitted
         */
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,
        @DecimalMin(value = "0.0", message = "Deposit cannot be negative")
        BigDecimal deposit,
        @Min(value = 1, message = "Payment due day must be between 1 and 31")
        @Max(value = 31, message = "Payment due day must be between 1 and 31")
        Integer paymentDueDay,
        RentalStatus status,
        String notes
        ) {

}
