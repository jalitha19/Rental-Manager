package com.rental.dto;

import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.PropertyType;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertyResponse(
        Long id,
        String propertyCode,
        PropertyType type,
        String name,
        String address,
        BigDecimal monthlyRent,
        BigDecimal deposit,
        PropertyStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
