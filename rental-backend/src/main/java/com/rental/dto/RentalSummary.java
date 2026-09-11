package com.rental.dto;

public record RentalSummary(
        Long id,
        PropertySummary property,
        TenantSummary tenant
) {
}
