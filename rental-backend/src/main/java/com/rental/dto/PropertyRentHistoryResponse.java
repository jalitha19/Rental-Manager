package com.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyRentHistoryResponse(
        Long id,
        BigDecimal monthlyRent,
        LocalDate effectiveFrom
) {
}
