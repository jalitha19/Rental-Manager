package com.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OccupantResponse(
        Long id,
        TenantSummary tenant,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent
        ) {

}
