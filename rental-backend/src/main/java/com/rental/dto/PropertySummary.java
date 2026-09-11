package com.rental.dto;

import com.rental.entity.enums.PropertyType;

public record PropertySummary(
        Long id,
        String propertyCode,
        String name,
        PropertyType type
) {
}
