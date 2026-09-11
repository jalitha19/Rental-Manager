package com.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentPendingItem(
        Long rentPaymentId,
        Long rentalId,
        String tenantName,
        String propertyCode,
        String propertyName,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal balance,
        LocalDate dueDate,
        boolean overdue
) {
}
