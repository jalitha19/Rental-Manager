package com.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OccupantResponse(
        Long id,
        TenantSummary tenant,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        List<RentPeriodResponse> rentPeriods
) {}
