package com.rental.dto;

import com.rental.entity.enums.RentalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
        Instant updatedAt
        ) {

}
