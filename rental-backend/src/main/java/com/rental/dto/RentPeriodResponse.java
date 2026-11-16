package com.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Stored rate transitions are rendered as contiguous inclusive month ranges. */
public record RentPeriodResponse(LocalDate startMonth, LocalDate endMonth, BigDecimal monthlyRent) {}
