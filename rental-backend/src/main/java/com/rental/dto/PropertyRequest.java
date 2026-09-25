package com.rental.dto;

import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.PropertyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyRequest(
        @NotBlank(message = "Property code is required")
        String propertyCode,

        @NotNull(message = "Type is required")
        PropertyType type,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Address is required")
        String address,

        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,

        @DecimalMin(value = "0.0", message = "Deposit cannot be negative")
        BigDecimal deposit,

        PropertyStatus status,

        String notes,

        /**
         * Only used when monthlyRent is different from the property's current
         * rent. Marks the date the new standard rent takes effect for the
         * rate history. Defaults to today if not sent.
         */
        LocalDate rentEffectiveFrom
) {
}
