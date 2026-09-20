package com.rental.dto;

import com.rental.entity.enums.RentalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * startDate is the earliest occupant start date and monthlyRent is the total
 * of all occupants' rents. Per-person details are in "occupants".
 */
public record RentalResponse(
        Long id,
        PropertySummary property,
        TenantSummary tenant,
        TenantSummary tenant2,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        BigDecimal deposit,
        Integer paymentDueDay,
        RentalStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<OccupantResponse> occupants
        ) {

}
