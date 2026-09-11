package com.rental.dto;

import com.rental.entity.enums.RentalStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EndRentalRequest(
        @NotNull(message = "End date is required")
        LocalDate endDate,

        /** COMPLETED (normal move-out) or CANCELLED (rental never really happened) */
        @NotNull(message = "Status is required")
        RentalStatus status,

        String notes
) {
}
