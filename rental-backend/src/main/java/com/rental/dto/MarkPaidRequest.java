package com.rental.dto;

import com.rental.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarkPaidRequest(
        /** Optional -- defaults to the full amount due if omitted */
        @DecimalMin(value = "0.0", message = "Amount paid cannot be negative")
        BigDecimal amountPaid,

        /** Optional -- defaults to today */
        LocalDate paymentDate,

        PaymentMethod paymentMethod,

        String notes
) {
}
